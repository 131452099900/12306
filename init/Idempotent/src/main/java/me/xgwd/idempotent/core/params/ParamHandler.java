package me.xgwd.idempotent.core.params;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import me.xgwd.auth.core.UserContext;
import me.xgwd.base.exception.ClientException;
import me.xgwd.idempotent.bean.IdempotentParamWrapper;
import me.xgwd.idempotent.core.AbsHandler;
import me.xgwd.idempotent.core.Handler;
import me.xgwd.idempotent.core.IdempotentContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 */
@RequiredArgsConstructor
public class ParamHandler extends AbsHandler implements IdempotentParamService {

    private final RedissonClient redissonClient;
    private final static String LOCK = "lock:param:restAPI";
    @Override
    public IdempotentParamWrapper build(ProceedingJoinPoint joinPoint) {
        // 获取joinPoint的参数，然后进行上锁
        String lockKey = String.format("idempotent:path:%s:currentUserId:%s:md5:%s", getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint));
        return IdempotentParamWrapper.builder().lockKey(lockKey).joinPoint(joinPoint).build();
    }

    private String getServletPath() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest().getServletPath();
    }
    private String getCurrentUserId() {
        String userId = UserContext.getUserId();
        if(StrUtil.isBlank(userId)){
            throw new ClientException("用户ID获取失败，请登录");
        }
        return userId;
    }
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }


    // 幂等校验逻辑看
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 这个锁的生命周期是以接口为单位，对于一些分布式系统，一般得收到确认ack才能把他们删除不然中途可能还有可能会重复
        String lockKey = wrapper.getLockKey();
        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.tryLock()) {
            throw new ClientException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK, lock);
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void exceptionProcessing() {
        postProcessing();
    }
}
