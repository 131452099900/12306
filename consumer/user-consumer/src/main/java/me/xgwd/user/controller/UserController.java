package me.xgwd.user.controller;

import me.xgwd.api.user.PassengerService;
import me.xgwd.api.user.UserService;
import me.xgwd.auth.core.UserContext;
import me.xgwd.base.exception.ClientException;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.*;
import me.xgwd.common.enums.UserRegisterErrorCodeEnum;
import me.xgwd.web.res.Results;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/9:57
 * @Description:
 */
@RestController
public class UserController {
    @DubboReference(interfaceClass = UserService.class, check = false, filter = "dubboRpcFilter", retries = 0, timeout = 111110000)
    private UserService userService;

    @DubboReference(interfaceClass = PassengerService.class, check = false, filter = "dubboRpcFilter", retries = 0, timeout = 111110000)
    private PassengerService passengerService;
    @GetMapping("/user")
    public void t() {
        userService.test();
    }


    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return userService.login(requestParam);
    }

    @PostMapping("/api/user-service/register")
    public Result<UserRegisterRespDTO> register(@RequestBody UserRegisterReqDTO requestParam) {
        return userService.register(requestParam);
    }

    @GetMapping("/api/user-service/check-login")
    public Result<UserLoginRespDTO> checkLogin(@RequestParam("accessToken") String accessToken) {
        return userService.checkLogin(accessToken);
    }

    /**
     * 根据用户名查询用户脱敏信息
     */
    @GetMapping("/api/user-service/query")
    public Result<UserQueryRespDTO> queryUserByUsername(@RequestParam("username") String username) {
        return userService.queryUserByUsername(username);
    }

    /**
     * 根据用户名查询用户无脱敏信息
     */
    @GetMapping("/api/user-service/actual/query")
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") String username) {
        return userService.queryActualUserByUsername(username);
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/api/user-service/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return userService.hasUsername(username);
    }

    /**
     * * 修改用户
     * @param requestParam
     * @return
     */
    @PostMapping("/api/user-service/update")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {

        return userService.update(requestParam);
    }

    /**
     * * 注销用户
     * @param requestParam
     * @return
     */

    @PostMapping("/api/user-service/deletion")
    public Result<Void> deletion(@RequestBody UserDeletionReqDTO requestParam) {
        String username = requestParam.getUsername();
        if (StringUtils.isBlank(username)) throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
        if (!username.equals(UserContext.getUsername())) {
            System.out.println(UserContext.getUsername());
            System.out.println(username);
            throw new ClientException("注销账号与登录账号不一致");
        }
        return userService.deletion(requestParam);
    }

    /**
     * 用户退出登录
     */
    @GetMapping("/api/user-service/logout")
    public Result<Void> logout(@RequestParam(required = false) String accessToken) {
        return userService.logout(accessToken);
    }



}
