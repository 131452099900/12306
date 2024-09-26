package me.xgwd.api.ticket;

import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.bean.dto.TicketPurchaseRespDTO;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/15:13
 * @Description:
 */
public interface TicketService {

    TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam);


}
