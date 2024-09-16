package me.xgwd.idempotent.core;

import me.xgwd.idempotent.annotation.Idemotent;
import me.xgwd.idempotent.bean.IdempotentParamWrapper;
import org.aspectj.lang.ProceedingJoinPoint;



public abstract class AbsHandler implements Handler{

    /* 留给抽象子类定义 */
    public abstract IdempotentParamWrapper build(ProceedingJoinPoint joinPoint);


    @Override
    public void execute(ProceedingJoinPoint joinPoint, Idemotent idempotent) {
        IdempotentParamWrapper wrapper = build(joinPoint);
        wrapper.setIdempotent(idempotent);
        handler(wrapper);
    }
}
