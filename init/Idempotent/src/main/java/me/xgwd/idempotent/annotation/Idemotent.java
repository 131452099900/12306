package me.xgwd.idempotent.annotation;

import me.xgwd.idempotent.enums.IdempotentSceneEnum;
import me.xgwd.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author 86135
 */

@Retention(RetentionPolicy.RUNTIME)
@Documented // 保留注解
public @interface Idemotent {

    String key() default "";

    String message() default "你的操作太快了，请稍后重试";

    IdempotentSceneEnum sence() default IdempotentSceneEnum.RESTAPI;

    IdempotentTypeEnum type() default IdempotentTypeEnum.PARAM;

    String prefix() default "";

    // 防重令牌的重复时间
    long timeout() default 3600L;
}
