package me.xgwd.order.orderservice;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.xgwd.order.dao.bean.OrderItemPassengerDO;
import me.xgwd.order.mapper.OrderItemPassengerMapper;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/22:15
 * @Description:
 */
@Component
public class OrderPassengerRelationService extends ServiceImpl<OrderItemPassengerMapper, OrderItemPassengerDO> {
}
