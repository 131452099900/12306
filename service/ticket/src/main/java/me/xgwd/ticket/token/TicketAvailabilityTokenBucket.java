package me.xgwd.ticket.token;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.bean.doain.PurchaseTicketPassengerDetailDTO;
import me.xgwd.bean.doain.RouteDTO;
import me.xgwd.bean.doain.SeatTypeCountDTO;
import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.VehicleTypeEnum;
import me.xgwd.common.util.Assert;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.handler.purchase.bean.TrainDO;
import me.xgwd.ticket.mapper.TrainMapper;
import me.xgwd.ticket.sservice.SeatService;
import me.xgwd.ticket.sservice.TrainStationService;
import me.xgwd.ticket.token.dto.TokenResultDTO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 列车车票余量令牌桶，应对海量并发场景下满足并行、限流以及防超卖等场景
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAvailabilityTokenBucket {

    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final SeatService seatService;
    private final TrainMapper trainMapper;

    private static final String LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH = "lua/ticket_availability_token_bucket.lua";
    private static final String LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH = "lua/ticket_availability_rollback_token_bucket.lua";

    public TokenResultDTO takeTokenFromBucket(PurchaseTicketReqDTO requestParam) {
        // 获取列车缓存
        TrainDO trainDO = distributedCache.safeGet(
                RedisKeyConstant.TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()),
                15,
                TimeUnit.DAYS);

        // 获取列车的所有站点route 15个
        String jsonRouteArray = distributedCache.safeGet(RedisKeyConstant.TRAIN_ROUTE_KEY + trainDO.getId(), String.class, () -> {
            List<RouteDTO> routeDTOS = trainStationService
                    .listTrainStationRoute(requestParam.getTrainId(), trainDO.getStartStation(), trainDO.getEndStation());
            return JSONUtil.toJsonStr(routeDTOS);
        }, 15, TimeUnit.DAYS);
        List<RouteDTO> routeDTOList = JSONArray.parseArray(jsonRouteArray, RouteDTO.class);


        // 判断
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 列车的预扣减令牌桶key
        String tokenBucketHashKey = RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET  + requestParam.getTrainId();

        distributedCache.delete(tokenBucketHashKey);
        Boolean hasKey = distributedCache.hasKey(tokenBucketHashKey);

        // 如果没有初始化就初始化
        if (!hasKey) {
            RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET, requestParam.getTrainId()));
            if (!lock.tryLock()) {
                throw new ServiceException("购票异常，请稍候再试");
            }

            try {
                Boolean hasKeyTwo = distributedCache.hasKey(tokenBucketHashKey);
                if (!hasKeyTwo) {
                    // 列车的所有座位类型 如果是东车就有4中 3 4 5 13
                    List<Integer> seatTypes = VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType());
                    Map<String, String> ticketAvailabilityTokenMap = new HashMap<>();
                    for (RouteDTO each : routeDTOList) {
                        // 根据列车ID，route的起始和终点还有座位类型 获取 当前route的seatType类型还有多少个座位
                        // 这里有3个 3:92 4:192 5:216
                        List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listSeatTypeCount(Long.parseLong(requestParam.getTrainId()), each.getStartStation(), each.getEndStation(), seatTypes);

                        for (SeatTypeCountDTO eachSeatTypeCountDTO : seatTypeCountDTOList) {
                            // 起始_终点_seatType
                            String buildCacheKey = StrUtil.join("_", each.getStartStation(), each.getEndStation(), eachSeatTypeCountDTO.getSeatType());
                            System.out.println(buildCacheKey + " key");
                            ticketAvailabilityTokenMap.put(buildCacheKey, String.valueOf(eachSeatTypeCountDTO.getSeatCount()));

                        }
                    }
                    // 把所有route的座位token放进redis
                    // 各个route的hash结构 剩余数量 列车:起始_终点_seatType:数量
                    stringRedisTemplate.opsForHash().putAll(tokenBucketHashKey, ticketAvailabilityTokenMap);
                }
            } finally {
                lock.unlock();
            }
        }
        // 执行lua脚本
        DefaultRedisScript<String> actual = Singleton.get(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH)));
            redisScript.setResultType(String.class);
            return redisScript;
        });
        Assert.notNull(actual);

        // 本次订单需要的各种座位的数量
        Map<Integer, Long> seatTypeCountMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType, Collectors.counting()));
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));

        // 获取开始站点到结束站点的所有站点route信息
        List<RouteDTO> takeoutRouteDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());

        // tokenBucketHashKey:列车IDKEY luaScriptKey:起始站-终点站 每种座位类型的数量封装成jsonArray takeoutRouteDTOList:所有route信息
        // lua脚本的执行逻辑 获取判断各个类型的座位在本车次有足够的座位，同时做一个扣减的逻辑，对各个route进行扣减库存
        // 如果不够就返回 3_1 也就是3这种座位数量缺少1
        String resultStr = stringRedisTemplate.execute(actual, Lists.newArrayList(tokenBucketHashKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutRouteDTOList), luaScriptKey);
        TokenResultDTO result = JSON.parseObject(resultStr, TokenResultDTO.class);
        return result == null
                ? TokenResultDTO.builder().tokenIsNull(Boolean.TRUE).build()
                : result;
    }

    /**
     * * 当发生不一致时进行删除token
     * @param requestParam
     */
    public void delTokenInBucket(PurchaseTicketReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        String tokenBucketHashKey = RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        stringRedisTemplate.delete(tokenBucketHashKey);
    }
}
