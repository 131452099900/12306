package me.xgwd.api.user;

import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.*;

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

    Result<UserLoginRespDTO> login(UserLoginReqDTO requestParam);

    Result<UserLoginRespDTO> checkLogin(String accessToken);

    Result<UserQueryRespDTO> queryUserByUsername(String username);

    Result<UserQueryActualRespDTO> queryActualUserByUsername(String username);

    Result<Boolean> hasUsername(String username);

    Result<Void> update(UserUpdateReqDTO requestParam);

    Result<Void> deletion(UserDeletionReqDTO requestParam);

    Result<Void> logout(String accessToken);
}
