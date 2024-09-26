package me.xgwd.ticket.sservice.strage;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/17:19
 * @Description:
 */

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.bean.doain.RouteDTO;
import me.xgwd.bean.doain.TrainSeatBaseDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.partten.strategy.AbstractExecuteStrategy;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.token.dto.SelectSeatDTO;
import me.xgwd.ticket.token.dto.TrainPurchaseTicketRespDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import me.xgwd.ticket.sservice.TrainStationService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 抽象高铁购票模板基础服务
 */
@Component
public abstract class AbstractTrainPurchaseTicketTemplate implements IPurchaseTicket, CommandLineRunner, AbstractExecuteStrategy<SelectSeatDTO, List<TrainPurchaseTicketRespDTO>> {

    private DistributedCache distributedCache;
    private String ticketAvailabilityCacheUpdateType;
    private TrainStationService trainStationService;

    /**
     * 选择座位
     *
     * @param requestParam 购票请求入参
     * @return 乘车人座位
     */
    protected abstract List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam);

    protected TrainSeatBaseDTO buildTrainSeatBaseDTO(SelectSeatDTO requestParam) {
        return TrainSeatBaseDTO.builder()
                .trainId(requestParam.getRequestParam().getTrainId())
                .departure(requestParam.getRequestParam().getDeparture())
                .arrival(requestParam.getRequestParam().getArrival())
                .chooseSeatList(requestParam.getRequestParam().getChooseSeats())
                .passengerSeatDetails(requestParam.getPassengerSeatDetails())
                .build();
    }

    /**
     * * 策略模式执行方法
     */
    @Override
    public List<TrainPurchaseTicketRespDTO> executeResp(SelectSeatDTO requestParam) {
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);


        // 扣减车厢余票缓存，扣减站点余票缓存 如果不用binlong就直接更新redis不过没有一致性
        if (CollUtil.isNotEmpty(actualResult) && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            String trainId = requestParam.getRequestParam().getTrainId();
            String departure = requestParam.getRequestParam().getDeparture();
            String arrival = requestParam.getRequestParam().getArrival();
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

            // hash进行对涉及的进行删除 涉及的中间件
            routeDTOList.forEach(each -> {
                // trainId_startStation_endStation 扣减库存
                String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                stringRedisTemplate.opsForHash().increment(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(requestParam.getSeatType()), -actualResult.size());
            });
        }
        return actualResult;
    }

    @Override
    public void run(String... args) throws Exception {
        distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
        trainStationService = ApplicationContextHolder.getBean(TrainStationService.class);
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        ticketAvailabilityCacheUpdateType = configurableEnvironment.getProperty("ticket.availability.cache-update.type", "");
    }
}
