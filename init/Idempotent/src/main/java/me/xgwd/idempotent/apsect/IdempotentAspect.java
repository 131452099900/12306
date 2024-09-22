package me.xgwd.idempotent.apsect;


import me.xgwd.idempotent.annotation.Idemotent;
import me.xgwd.idempotent.core.Handler;
import me.xgwd.idempotent.core.IdempotentContext;
import me.xgwd.idempotent.core.IdempotentExecuteHandlerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public final class IdempotentAspect {

    @Around("@annotation(me.xgwd.idempotent.annotation.Idemotent)")
    public Object idempotenHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        Idemotent idempotent = getIdempotent(joinPoint);

        // 根据场景和类型获取对应处理器
        Handler handler = IdempotentExecuteHandlerFactory.getInstance(idempotent.sence(), idempotent.type());
        Object resultObj;
        try {
            handler.execute(joinPoint, idempotent);
            resultObj = joinPoint.proceed();

            // 处理完删除锁 这里有点不一样？ 消费完了下一个接口若存在 则还会进行重复的消费，只是在消费过程中无法重复消费而已
            // 所以锁的生命周期应该和消息重发的区间段，而不是
            handler.postProcessing();
        } catch (RepeatConsumptionException ex) {
            /**
             * 触发幂等逻辑时可能有两种情况：
             *    * 1. 消息还在处理，但是不确定是否执行成功，那么需要返回错误，方便 RocketMQ 再次通过重试队列投递
             *    * 2. 消息处理成功了，该消息直接返回成功即可
             */
            if (!ex.getError()) {
                return null;
            }
            throw ex;
        }catch (Throwable throwable) {
            // 客户端消费存在异常，需要删除幂等标识方便下次 RocketMQ 再次通过重试队列投递
            handler.exceptionProcessing();
            throw throwable;
        } finally {
            IdempotentContext.clean();
        }

        return resultObj;
    }

    /**
     * * 获取方法上的注解
     * @param joinPoint
     * @return
     * @throws NoSuchMethodException
     */
    public static Idemotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method declaredMethod = joinPoint.getTarget().getClass().getDeclaredMethod
                (signature.getName(), signature.getMethod().getParameterTypes());
        return declaredMethod.getAnnotation(Idemotent.class);
    }
}
