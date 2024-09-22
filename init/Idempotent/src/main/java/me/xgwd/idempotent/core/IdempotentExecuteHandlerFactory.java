package me.xgwd.idempotent.core;

import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.idempotent.core.params.IdempotentParamService;
import me.xgwd.idempotent.core.token.IdempotentTokenService;
import me.xgwd.idempotent.enums.IdempotentSceneEnum;
import me.xgwd.idempotent.enums.IdempotentTypeEnum;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/16/19:59
 * @Description:
 */
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取幂等执行处理器
     *
     * @param scene 指定幂等验证场景类型
     * @param type  指定幂等处理类型
     * @return 幂等执行处理器
     */
    public static Handler getInstance(IdempotentSceneEnum scene, IdempotentTypeEnum type) {
        Handler result = null;
        switch (scene) {
            case RESTAPI -> {
                switch (type) {
                    case PARAM -> result = ApplicationContextHolder.getBean(IdempotentParamService.class);
                    case TOKEN -> result = ApplicationContextHolder.getBean(IdempotentTokenService.class);
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
        return result;
    }
}

