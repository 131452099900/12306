package me.xgwd.bean.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/16:40
 * @Description:
 */
@Data
public class UserRegisterRespDTO implements Serializable {

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;
}
