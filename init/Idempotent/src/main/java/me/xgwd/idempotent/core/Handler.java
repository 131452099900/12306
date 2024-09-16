package me.xgwd.idempotent.core;

import me.xgwd.idempotent.annotation.Idemotent;
import me.xgwd.idempotent.bean.IdempotentParamWrapper;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 接口规范拓展定义
 *
 */
public interface Handler {

    /**
     * 幂等处理逻辑
     *
     * @param wrapper 幂等参数包装器
     */
    void handler(IdempotentParamWrapper wrapper);

    /**
     * 执行幂等处理逻辑
     *
     * @param joinPoint  AOP 方法处理
     * @param idempotent 幂等注解
     */
    void execute(ProceedingJoinPoint joinPoint, Idemotent idempotent);

    /**
     * 异常流程处理
     */
    default void exceptionProcessing() {

    }

    /**
     * 后置处理
     */
    default void postProcessing() {

    }
}
