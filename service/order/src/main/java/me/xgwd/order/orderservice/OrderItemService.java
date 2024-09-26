package me.xgwd.order.orderservice;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.xgwd.order.dao.bean.OrderItemDO;
import me.xgwd.order.mapper.OrderItemMapper;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/22:12
 * @Description:
 */
@Component
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItemDO> {
}
