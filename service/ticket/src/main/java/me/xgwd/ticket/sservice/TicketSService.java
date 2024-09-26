package me.xgwd.ticket.sservice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.order.OrderService;
import me.xgwd.api.ticket.TicketService;
import me.xgwd.api.user.UserService;
import me.xgwd.auth.core.UserContext;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.exception.RemoteException;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.doain.PurchaseTicketPassengerDetailDTO;
import me.xgwd.bean.dto.*;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.VehicleSeatTypeEnum;
import me.xgwd.common.enums.VehicleTypeEnum;
import me.xgwd.partten.strategy.AbstractStrategyChoose;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.enums.SourceEnum;
import me.xgwd.ticket.enums.TicketStatusEnum;
import me.xgwd.ticket.handler.purchase.bean.TicketDO;
import me.xgwd.ticket.handler.purchase.bean.TrainDO;
import me.xgwd.ticket.handler.purchase.bean.TrainStationPriceDO;
import me.xgwd.ticket.handler.purchase.bean.TrainStationRelationDO;
import me.xgwd.ticket.mapper.TicketMapper;
import me.xgwd.ticket.mapper.TrainMapper;
import me.xgwd.ticket.mapper.TrainStationPriceMapper;
import me.xgwd.ticket.mapper.TrainStationRelationMapper;
import me.xgwd.ticket.token.dto.SelectSeatDTO;
import me.xgwd.ticket.token.dto.TrainPurchaseTicketRespDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/16:13
 * @Description:
 */
@Component
@Slf4j
public class TicketSService extends ServiceImpl<TicketMapper, TicketDO> {
    @Autowired
    private DistributedCache distributedCache;
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private SeatService seatService;
    @Autowired
    private  AbstractStrategyChoose abstractStrategyChoose;
    @Autowired
    private  TrainStationPriceMapper trainStationPriceMapper;
    @Autowired
    private  TrainStationRelationMapper trainStationRelationMapper;
    @DubboReference(check = false, interfaceClass = UserService.class)
    private UserService userService;
    @DubboReference(check = false, interfaceClass = OrderService.class)
    private OrderService orderService;

    private final ExecutorService selectSeatThreadPoolExecutor = Executors.newFixedThreadPool(10);

    public TicketPurchaseRespDTO excutePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        String trainId = requestParam.getTrainId();
        TrainDO trainDO = distributedCache.safeGet(
                RedisKeyConstant.TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                15,
                TimeUnit.DAYS);

        // 选取座位
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = select(trainDO.getTrainType(), requestParam);
        // 保存车票
        List<TicketDO> ticketDOList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                        .username(requestParam.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .passengerId(each.getPassengerId())
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                        .build())
                .toList();
        saveBatch(ticketDOList);
        Result<String> ticketOrderResult;
        try {
            // 添加所有车票
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            trainPurchaseTicketResults.forEach(each -> {
                // 怎么两个一摸一样 车票订单详情创建请求参数
                System.out.println("asdasjdasdhas =======================> adjahsdjaskdha " + each.getIdCard());
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .phone(each.getPhone())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();
                // 车票订单详情返回参数
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .idCard(each.getIdCard())
                        .idType(each.getIdType())
                        .seatType(each.getSeatType())
                        .ticketType(each.getUserType())
                        .realName(each.getRealName())
                        .build();

                // 订单item详情 车票订单详情
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });

            // 远程调用order订单
            // 获取车票出发站点和结束站点的信息
            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());
            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);


            // 封装订单 发起远程调用进行保存订单
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    .source(SourceEnum.INTERNET.getCode()) // 线上购票
                    .trainNumber(trainDO.getTrainNumber())
                    .departureTime(trainStationRelationDO.getDepartureTime())
                    .arrivalTime(trainStationRelationDO.getArrivalTime())
                    .ridingDate(trainStationRelationDO.getDepartureTime())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .username(requestParam.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId())) // 车次ID
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList) // 所有乘车人车票
                    .build();

            ticketOrderResult = orderService.createTicketOrder(orderCreateRemoteReqDTO);
            if (!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())) {
                log.error("订单服务调用失败，返回结果：{}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }

        } catch (Exception ex) {
            log.error("远程调用订单服务创建错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }

        // 返回一个订单号和乘车人订单详情列表
        return new TicketPurchaseRespDTO(ticketOrderResult.getData(), ticketOrderDetailResults);
    }

    private List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        // 对座位分组获取各个种类座位的需要数量
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));

        List<TrainPurchaseTicketRespDTO> actualResult = new CopyOnWriteArrayList<>();
        // 出现多种时，进行并行处理，否则直接单线程 逻辑主要在distributeSeats
        if (seatTypeMap.size() > 1) {
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>();
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 线程池参数如何设置？详情查看：https://nageoffer.com/12306/question
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor
                        .submit(() -> distributeSeats(trainType, seatType, requestParam, passengerSeatDetails));
                futureResults.add(completableFuture);
            });
            // 并行流极端情况下有坑，详情参考：https://nageoffer.com/12306/question
            futureResults.parallelStream().forEach(completableFuture -> {
                try {
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    // 回滚令牌
                    throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
                }
            });
        } else {
            for (Integer seatType : seatTypeMap.keySet()) {
                List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = seatTypeMap.get(seatType);
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(trainType, seatType, requestParam, passengerSeatDetails);
                actualResult.addAll(aggregationResult);
            }
        }

        // 上面操作是把座位选择出来 并且对redis里面涉及的route进行库存扣减

        //
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());
        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList = null;
        try {
            // 校验是否合规乘车人
            try {
                passengerRemoteResult = userService.listPassengerQueryByIds(requestParam.getUsername(), passengerIds);
                if (!passengerRemoteResult.isSuccess() || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                    throw new RemoteException("用户服务远程调用查询乘车人相关信息错误");
                }
            } catch (Throwable ex) {
                if (ex instanceof RemoteException) {
                    log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", requestParam.getUsername(), passengerIds);
                } else {
                    log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", requestParam.getUsername(), passengerIds, ex);
                }
                throw ex;
            }
        } catch (Throwable throwable) {
            // 出发token回调
        }

        // 补充actualResult的车票信息 因为上面只有那个啥座位的
        List<PassengerRespDTO> finalPassengerRemoteResultList = passengerRemoteResultList;

        actualResult.forEach(each -> {
            String passengerId = each.getPassengerId();
            finalPassengerRemoteResultList.stream()
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    .findFirst()
                    .ifPresent(passenger -> {
                        each.setIdCard(passenger.getIdCard());
                        each.setPhone(passenger.getPhone());
                        each.setUserType(passenger.getDiscountType());
                        each.setIdType(passenger.getIdType());
                        each.setRealName(passenger.getRealName());
                    });
            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    .select(TrainStationPriceDO::getPrice);
            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
            each.setAmount(trainStationPriceDO.getPrice());
        });

        // 锁定选中以及沿途车票状态
        seatService.lockSeat(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), actualResult);
        return actualResult;

    }


    private List<TrainPurchaseTicketRespDTO> distributeSeats(Integer trainType, Integer seatType, PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {
        // passengerSeatDetails对应的编码
        // 交通类型+座位类型
        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);
        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                .seatType(seatType)
                .passengerSeatDetails(passengerSeatDetails)
                .requestParam(requestParam)
                .build();
        try {
            // 具体选择逻辑
            List<TrainPurchaseTicketRespDTO> res1 = abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
            return res1;
        } catch (ServiceException ex) {
            throw new ServiceException("当前车次列车类型暂未适配，请购买G35或G39车次");
        }
    }


}
