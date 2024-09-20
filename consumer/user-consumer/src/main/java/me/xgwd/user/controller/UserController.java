package me.xgwd.user.controller;

import me.xgwd.api.user.UserService;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.UserRegisterReqDTO;
import me.xgwd.bean.dto.UserRegisterRespDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/9:57
 * @Description:
 */
@RestController
public class UserController {

    @DubboReference(interfaceClass = UserService.class, check = false)
    private UserService userService;

    @GetMapping("/user")
    public void t() {
        userService.test();
    }


    @PostMapping("/register")
    public Result<UserRegisterRespDTO> register(@RequestBody UserRegisterReqDTO requestParam) {

        return userService.register(requestParam);
    }
}
