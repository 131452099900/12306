package me.xgwd.ticket.sservice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.xgwd.bean.doain.RouteDTO;
import me.xgwd.bean.doain.SeatTypeCountDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.RedisConstant;
import me.xgwd.common.enums.SeatStatusEnum;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.handler.purchase.bean.SeatDO;
import me.xgwd.ticket.mapper.SeatMapper;
import me.xgwd.ticket.token.dto.TrainPurchaseTicketRespDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/0:18
 * @Description:
 */
@Component
public class SeatService {

    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private DistributedCache distributedCache;
    @Autowired
    private TrainStationService trainStationService;
    public List<SeatTypeCountDTO> listSeatTypeCount(Long trainId, String startStation, String endStation, List<Integer> seatTypes) {
        List<SeatTypeCountDTO> seatTypeCountDTOS = seatMapper.listSeatTypeCount(trainId, startStation, endStation, seatTypes);
        return seatTypeCountDTOS;
    }

    public List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, carriageType)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .groupBy(SeatDO::getCarriageNumber)
                .select(SeatDO::getCarriageNumber);
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);
        return seatDOList.stream().map(SeatDO::getCarriageNumber).collect(Collectors.toList());
    }

    /**
     * 获取列车车厢余票集合
     *
     * @param trainId           列车 ID
     * @param departure         出发站
     * @param arrival           到达站
     * @param trainCarriageList 车厢编号集合
     * @return 车厢余票集合
     */
    public List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival, List<String> trainCarriageList) {
        String keySuffix = StrUtil.join("_", trainId, departure, arrival);
        if (distributedCache.hasKey(RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix)) {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            List<Object> trainStationCarriageRemainingTicket =
                    stringRedisTemplate.opsForHash().multiGet(RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix, Arrays.asList(trainCarriageList.toArray()));
            if (CollUtil.isNotEmpty(trainStationCarriageRemainingTicket)) {
                return trainStationCarriageRemainingTicket.stream().map(each -> Integer.parseInt(each.toString())).collect(Collectors.toList());
            }
        }
        SeatDO seatDO = SeatDO.builder()
                .trainId(Long.parseLong(trainId))
                .startStation(departure)
                .endStation(arrival)
                .build();
        return seatMapper.listSeatRemainingTicket(seatDO, trainCarriageList);
    }

    public List<String> listAvailableSeat(String trainId, String carriageNumber, Integer seatType, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getCarriageNumber, carriageNumber)
                .eq(SeatDO::getSeatType, seatType)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .select(SeatDO::getSeatNumber);
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);
        return seatDOList.stream().map(SeatDO::getSeatNumber).collect(Collectors.toList());
    }


    public void lockSeat(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespList) {
        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);
        trainPurchaseTicketRespList.forEach(each -> routeList.forEach(item -> {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getStartStation, item.getStartStation())
                    .eq(SeatDO::getEndStation, item.getEndStation())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());

            // 锁定状态，并不是已售状态
            SeatDO updateSeatDO = SeatDO.builder()
                    .seatStatus(SeatStatusEnum.LOCKED.getCode())
                    .build();
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }
}
