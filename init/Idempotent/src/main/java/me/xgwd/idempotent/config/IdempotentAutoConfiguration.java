package me.xgwd.idempotent.config;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/16/20:24
 * @Description:
 */

import me.xgwd.cache.DistributedCache;
import me.xgwd.idempotent.apsect.IdempotentAspect;
import me.xgwd.idempotent.core.params.IdempotentParamService;
import me.xgwd.idempotent.core.params.ParamHandler;
import me.xgwd.idempotent.core.spel.IdempotentSpELByRestAPIExecuteHandler;
import me.xgwd.idempotent.core.spel.IdempotentSpELService;
import me.xgwd.idempotent.core.token.IdempotentTokenController;
import me.xgwd.idempotent.core.token.IdempotentTokenExecuteHandler;
import me.xgwd.idempotent.core.token.IdempotentTokenService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 幂等自动装配
 */
@Configuration
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAutoConfiguration {

    /**
     * 幂等切面
     */
    @Bean
    public IdempotentAspect idempotentAspect() {
        return new IdempotentAspect();
    }

    /**
     * 参数方式幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentParamService idempotentParamExecuteHandler(RedissonClient redissonClient) {
        return new ParamHandler(redissonClient);
    }

    /**
     * Token 方式幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentTokenService idempotentTokenExecuteHandler(DistributedCache distributedCache,
                                                                IdempotentProperties idempotentProperties) {
        return new IdempotentTokenExecuteHandler(distributedCache, idempotentProperties);
    }

    /**
     * 申请幂等 Token 控制器，基于 RestAPI 场景
     */
    @Bean
    public IdempotentTokenController idempotentTokenController(IdempotentTokenService idempotentTokenService) {
        return new IdempotentTokenController(idempotentTokenService);
    }

    /**
     * SpEL 方式幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentSpELService idempotentSpELByRestAPIExecuteHandler(RedissonClient redissonClient) {
        return new IdempotentSpELByRestAPIExecuteHandler(redissonClient);
    }
//
//    /**
//     * SpEL 方式幂等实现，基于 MQ 场景
//     */
//    @Bean
//    @ConditionalOnMissingBean
//    public IdempotentSpELByMQExecuteHandler idempotentSpELByMQExecuteHandler(DistributedCache distributedCache) {
//        return new IdempotentSpELByMQExecuteHandler(distributedCache);
//    }
}
