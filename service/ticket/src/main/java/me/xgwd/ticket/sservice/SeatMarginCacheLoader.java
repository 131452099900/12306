package me.xgwd.ticket.sservice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.lettuce.core.RedisClient;
import me.xgwd.bean.doain.RouteDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.cache.toolkit.CacheUtil;
import me.xgwd.common.enums.RedisConstant;
import me.xgwd.common.enums.SeatStatusEnum;
import me.xgwd.common.enums.VehicleTypeEnum;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.handler.purchase.bean.SeatDO;
import me.xgwd.ticket.handler.purchase.bean.TrainDO;
import me.xgwd.ticket.mapper.SeatMapper;
import me.xgwd.ticket.mapper.TrainMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/15:48
 * @Description:
 */
@Component
public class SeatMarginCacheLoader {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private DistributedCache distributedCache;
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private TrainStationService trainStationService;

    @Autowired
    private SeatMapper seatMapper;

    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        // 存放 route:seatType:count的
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();

        String keySuffix = CacheUtil.buildKey(trainId);
        String remainKey = CacheUtil.buildKey(trainId, departure, arrival);
        // 对车次载入加锁
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();

        try{
            // 车次站点余票查询，Key Prefix + 列车ID_起始站点_终点
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            // 余票key train_station_remaining_ticket:3::
            Object quantityObj = stringRedisTemplate.opsForHash().get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + remainKey, seatType);

            if (CacheUtil.isNullOrBlank(quantityObj)) {
                TrainDO trainDO = distributedCache.safeGet(
                        RedisKeyConstant.TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        15,
                        TimeUnit.DAYS
                );
                // 把该车次所有起点到终点全部查出来 比如 起点-中间-中间-终点
                String jsonRouteArray = distributedCache.safeGet(RedisKeyConstant.TRAIN_ROUTE_KEY + trainDO.getId(), String.class, () -> {
                    List<RouteDTO> routeDTOS = trainStationService
                            .listTrainStationRoute(trainId, trainDO.getStartStation(), trainDO.getEndStation());
                    return JSONUtil.toJsonStr(routeDTOS);
                }, 15, TimeUnit.DAYS);
                List<RouteDTO> routeDTOList = JSONArray.parseArray(jsonRouteArray, RouteDTO.class);

                if (CollUtil.isNotEmpty(routeDTOList)) {
                    switch (trainDO.getTrainType()) {

                        case 0 -> { // 高铁
                            // 车次各个旅程的座位数量
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 0商务 1一等 2二等
                                trainStationRemainingTicket.put("0", selectSeatMargin(trainId, 0, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("1", selectSeatMargin(trainId, 1, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("2", selectSeatMargin(trainId, 2, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        case 1 -> { // 动车

                            // 各个route都是
                            for (RouteDTO each : routeDTOList) {
                                // 3二等包座  4一等卧 5二等卧
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 二等座92个
                                trainStationRemainingTicket.put("3", selectSeatMargin(trainId, 3, each.getStartStation(), each.getEndStation()));
                                // 192个
                                trainStationRemainingTicket.put("4", selectSeatMargin(trainId, 4, each.getStartStation(), each.getEndStation()));
                                // 216个
                                trainStationRemainingTicket.put("5", selectSeatMargin(trainId, 5, each.getStartStation(), each.getEndStation()));
                                // 0个
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        case 2 -> { // 普通车
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 6软卧 7硬卧 8软座 13无座
                                trainStationRemainingTicket.put("6", selectSeatMargin(trainId, 6, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("7", selectSeatMargin(trainId, 7, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("8", selectSeatMargin(trainId, 8, each.getStartStation(), each.getEndStation()));
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                    }
                } else {
                    // 按对应列车类型座位全部返回0
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    VehicleTypeEnum.findSeatTypesByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemainingTicketMaps.put(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }

            }

            // route对应的全部座位类型的数量放入缓存
            // key(route) seatType:count
            for (String key : trainStationRemainingTicketMaps.keySet()){
                // 3_德州海宁 : 3（二号座位置）: 192
                stringRedisTemplate.opsForHash().putAll(key, trainStationRemainingTicketMaps.get(key));
            }

        } finally {
            lock.unlock();
        }
        return Optional.ofNullable(trainStationRemainingTicketMaps.get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + remainKey))
                .orElse(new LinkedHashMap<>());
    }

    /**
     * * 计算车次（起始-终点）对应座位类型有多少数量
     * * select * from t_seat where trainId = ? an seatType = type and startStation and  EndStation
     * @param trainId 车次
     * @param type  座位类型
     * @param departure 起始
     * @param arrival   终点
     * @return
     */
    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}
