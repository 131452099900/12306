package me.xgwd.user.controller;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.PhoneUtil;
import me.xgwd.api.user.PassengerService;
import me.xgwd.auth.core.UserContext;
import me.xgwd.base.exception.ClientException;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.PassengerActualRespDTO;
import me.xgwd.bean.dto.PassengerRemoveReqDTO;
import me.xgwd.bean.dto.PassengerReqDTO;
import me.xgwd.bean.dto.PassengerRespDTO;
import me.xgwd.idempotent.annotation.Idemotent;
import me.xgwd.idempotent.enums.IdempotentSceneEnum;
import me.xgwd.idempotent.enums.IdempotentTypeEnum;
import me.xgwd.web.res.Results;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/14:18
 * @Description:
 */

@RestController
public class PassengerController {

    @DubboReference(interfaceClass = PassengerService.class, check = false, filter = "dubboRpcFilter", retries = 0, timeout = 111110000)
    private PassengerService passengerService;

    /**
     * 获取乘车人信息，一个用户可能存在多乘车人
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername() {
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }

    /**
     * * 根据username和ids批量查询
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    public Result<List<PassengerActualRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids") List<Long> ids) {
        return Results.success(passengerService.listPassengerQueryByIds(username, ids));
    }

    @Idemotent(
            prefix = "12306:lock_passenger-alter:",
            key = "T(me.xgwd.auth.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            sence = IdempotentSceneEnum.RESTAPI,
            message = "正在添加乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/save")
    public Result<Void> savePassenger(@RequestBody PassengerReqDTO requestParam) {
        verifyPassenger(requestParam);
        passengerService.savePassenger(requestParam, UserContext.getUsername());
        return Results.success();
    }

    /**
     * * 修改乘车人信息
     */
    @Idemotent(
            prefix = "12306:lock_passenger-alter:",
            key = "T(me.xgwd.auth.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            sence = IdempotentSceneEnum.RESTAPI,
            message = "正在修改乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/update")
    public Result<Void> updatePassenger(@RequestBody PassengerReqDTO requestParam) {
        verifyPassenger(requestParam);
        requestParam.setUsername(UserContext.getUsername());
        passengerService.updatePassenger(requestParam);
        return Results.success();
    }

    @Idemotent(
            prefix = "12306:lock_passenger-alter:",
            key = "T(me.xgwd.auth.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            sence = IdempotentSceneEnum.RESTAPI,
            message = "正在删除乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/remove")
    public Result<Void> removePassenger(@RequestBody PassengerRemoveReqDTO requestParam) {
        requestParam.setUsername(UserContext.getUsername());
        passengerService.removePassenger(requestParam);
        return Results.success();
    }

    private void verifyPassenger(PassengerReqDTO requestParam) {
        int length = requestParam.getRealName().length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置2-16位的长度");
        }
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("乘车人证件号错误");
        }
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}
