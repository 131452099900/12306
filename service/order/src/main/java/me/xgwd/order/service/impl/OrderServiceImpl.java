package me.xgwd.order.service.impl;

import me.xgwd.api.order.OrderService;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.TicketOrderCreateRemoteReqDTO;
import me.xgwd.bean.dto.TicketOrderItemCreateRemoteReqDTO;
import me.xgwd.order.dao.bean.OrderDO;
import me.xgwd.order.dao.bean.OrderItemDO;
import me.xgwd.order.dao.bean.OrderItemPassengerDO;
import me.xgwd.order.dao.id.OrderIdGeneratorManager;
import me.xgwd.order.enums.OrderStatusEnum;
import me.xgwd.order.mapper.OrderMapper;
import me.xgwd.order.orderservice.OrderItemService;
import me.xgwd.order.orderservice.OrderPassengerRelationService;
import me.xgwd.web.res.Results;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 */
@DubboService(filter = "dubboRpcFilter")
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper = ApplicationContextHolder.getBean(OrderMapper.class);
    private final OrderItemService orderItemService = ApplicationContextHolder.getBean(OrderItemService.class);
    private final OrderPassengerRelationService orderPassengerRelationService =  ApplicationContextHolder.getBean(OrderPassengerRelationService.class);;
    @Override
    public Result<String> createTicketOrder(TicketOrderCreateRemoteReqDTO requestParam) {
        // 通过基因法将用户 ID 融入到订单号 也就是用户的后6位置
        String orderSn = OrderIdGeneratorManager.generateId(Long.parseLong(requestParam.getUserId()));
        OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .departureTime(requestParam.getDepartureTime())
                .ridingDate(requestParam.getRidingDate())
                .arrivalTime(requestParam.getArrivalTime())
                .trainNumber(requestParam.getTrainNumber())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(String.valueOf(requestParam.getUserId()))
                .build();
        orderMapper.insert(orderDO);


        // 插入order_ticket_item 也就是order和ticket和passenger的一对多关系
        List<TicketOrderItemCreateRemoteReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .orderSn(orderSn)
                    .phone(each.getPhone())
                    .seatType(each.getSeatType())
                    .username(requestParam.getUsername()).amount(each.getAmount()).carriageNumber(each.getCarriageNumber())
                    .idCard(each.getIdCard())
                    .ticketType(each.getTicketType())
                    .idType(each.getIdType())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .status(0)
                    .build();
            orderItemDOList.add(orderItemDO);
            OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();
            orderPassengerRelationDOList.add(orderPassengerRelationDO);
        });
        orderItemService.saveBatch(orderItemDOList);
        orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);

        // 发送延迟消息15分钟未支付取消订单
//        try {
//            // 发送 RocketMQ 延时消息，指定时间后取消订单
//            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
//                    .trainId(String.valueOf(requestParam.getTrainId()))
//                    .departure(requestParam.getDeparture())
//                    .arrival(requestParam.getArrival())
//                    .orderSn(orderSn)
//                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
//                    .build();
//            // 创建订单并支付后延时关闭订单消息怎么办？详情查看：https://nageoffer.com/12306/question
//            SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
//            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
//                throw new ServiceException("投递延迟关闭订单消息队列失败");
//            }
//        } catch (Throwable ex) {
//            log.error("延迟关闭订单消息队列发送错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
//            throw ex;
//        }
        return Results.success(orderSn);
    }
}
