package me.xgwd.api.order;

import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.TicketOrderCreateRemoteReqDTO;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/21:47
 * @Description:
 */
public interface OrderService {

    Result<String> createTicketOrder(TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO);
}
