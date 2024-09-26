package me.xgwd.ticket.sservice;

import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.common.enums.TicketChainMarkEnum;
import me.xgwd.partten.chain.AbstractChainContext;
import me.xgwd.ticket.common.RedisKeyConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/0:37
 * @Description:
 */
@Component
public class ChainService {
    @Autowired
    private AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketAbstractChainContext;

    public void handler(PurchaseTicketReqDTO requestParam) {
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);
    }
}
