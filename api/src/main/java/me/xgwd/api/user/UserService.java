package me.xgwd.api.user;

import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.UserRegisterReqDTO;
import me.xgwd.bean.dto.UserRegisterRespDTO;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/11:49
 * @Description:
 */
public interface UserService {
    void test();

    Result<UserRegisterRespDTO> register(UserRegisterReqDTO requestParam);
}
