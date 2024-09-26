package me.xgwd.ticket.controller;

import me.xgwd.api.ticket.TicketService;
import me.xgwd.api.user.PassengerService;
import me.xgwd.auth.core.UserContext;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.bean.dto.TicketPurchaseRespDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.web.res.Results;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/9:43
 * @Description:
 */
@RestController
public class TicketController {
    @DubboReference(timeout = 1000000000, version = "1.0", check = false)
    private TicketService ticketService;

    @PostMapping("/buy")
    public Result bugTicket(@RequestBody PurchaseTicketReqDTO requestParam) {
        requestParam.setUsername(UserContext.getUsername());
        long l = Long.parseLong(UserContext.getUserId());
        requestParam.setUserId(l);
        System.out.println(l + "asdasdvajdvawjdjasbxkz");
        TicketPurchaseRespDTO ticketPurchaseRespDTO = ticketService.purchaseTicketsV2(requestParam);
        return Results.success(ticketPurchaseRespDTO);
    }

    @Autowired
    private DistributedCache distributedCache;
    @GetMapping("/a")
    public void wq() {
        StringRedisTemplate instance =(StringRedisTemplate) distributedCache.getInstance();
        System.out.println(instance.opsForHash().get("ticket-service:ticket_availability_token_bucket:{3}", "德州_海宁_3"));
    }
}
