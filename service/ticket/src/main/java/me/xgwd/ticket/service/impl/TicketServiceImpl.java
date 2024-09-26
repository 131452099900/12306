package me.xgwd.ticket.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xgwd.api.ticket.TicketService;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.bean.doain.PurchaseTicketPassengerDetailDTO;
import me.xgwd.bean.doain.SeatTypeCountDTO;
import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.bean.dto.TicketPurchaseRespDTO;
import me.xgwd.partten.chain.AbstractChainContext;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.sservice.ChainService;
import me.xgwd.ticket.sservice.SeatService;
import me.xgwd.ticket.sservice.TicketSService;
import me.xgwd.ticket.token.TicketAvailabilityTokenBucket;
import me.xgwd.ticket.token.dto.TokenResultDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/15:12
 * @Description:
 */
@DubboService(version = "1.0")
public class TicketServiceImpl implements TicketService {
    private final ChainService chainService = ApplicationContextHolder.getBean(ChainService.class);
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket = ApplicationContextHolder.getBean(TicketAvailabilityTokenBucket.class);
    private  final RedissonClient redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
    private  final SeatService seatService = ApplicationContextHolder.getBean(SeatService.class);
    private  final TicketSService ticketSService = ApplicationContextHolder.getBean(TicketSService.class);
    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        // 1) 参数校验 1.车次ID 2.出发站-终点站 3.座位类型 4.乘车人列表 5.列车类型和标识
        // 2) 校验
        chainService.handler(requestParam);


        // 3)令牌桶
        TokenResultDTO tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
        // 如果数量不够了
        if (tokenResult.getTokenIsNull()) {
            Object ifPresentObj = tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId());
            if (ifPresentObj == null) {
                // 本地锁
                synchronized (TicketService.class) {
                    if (tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId()) == null) {
                        ifPresentObj = new Object();
                        tokenTicketsRefreshMap.put(requestParam.getTrainId(), ifPresentObj);
                        tokenIsNullRefreshToken(requestParam, tokenResult);
                    }
                }
            }
            throw new ServiceException("列车站点已无余票");
        }

        // 各种座位需要的锁
        List<RLock> distributedLockList = new ArrayList<>();
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        seatTypeMap.forEach((searType, count) -> {
            String lockKey = String.format(RedisKeyConstant.LOCK_PURCHASE_TICKETS_V2, requestParam.getTrainId(), searType);
            RLock distributedLock = redissonClient.getFairLock(lockKey);
            distributedLockList.add(distributedLock);
        });

        try {
            distributedLockList.forEach(RLock::lock);
            // 执行逻辑
            return ticketSService.excutePurchaseTickets(requestParam);
        } finally {
            // 释放锁
            distributedLockList.forEach(distributedLock -> {
                try {
                    distributedLock.unlock();
                } catch (Throwable ignored) {
                }
            });
        }

    }

    private final Cache<String, Object> tokenTicketsRefreshMap = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private final ScheduledExecutorService tokenIsNullRefreshExecutor = Executors.newScheduledThreadPool(1);


    /**
     * * 预先获取token失败，需要回滚token
     */
    private void tokenIsNullRefreshToken(PurchaseTicketReqDTO requestParam, TokenResultDTO tokenResult) {
        // 获取锁
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_TOKEN_BUCKET_ISNULL, requestParam.getTrainId()));
        if (!lock.tryLock()) {
            // 预警发送上报
            return;
        }

        tokenIsNullRefreshExecutor.schedule(() -> {
            try {
                List<Integer> seatTypes = new ArrayList<>();
                Map<Integer, Integer> tokenCountMap = new HashMap<>();
                tokenResult.getTokenIsNullSeatTypeCounts().stream()
                        .map(each -> each.split("_"))
                        .forEach(split -> {
                            int seatType = Integer.parseInt(split[0]);
                            seatTypes.add(seatType);
                            tokenCountMap.put(seatType, Integer.parseInt(split[1]));
                        });
                List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listSeatTypeCount(Long.parseLong(requestParam.getTrainId()), requestParam.getDeparture(), requestParam.getArrival(), seatTypes);
                for (SeatTypeCountDTO each : seatTypeCountDTOList) {
                    Integer tokenCount = tokenCountMap.get(each.getSeatType());
                    // 位置不足够时
                    if (tokenCount < each.getSeatCount()) {
                        ticketAvailabilityTokenBucket.delTokenInBucket(requestParam);
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
        }, 10, TimeUnit.SECONDS);
    }
}
