package me.xgwd.idgenerate;

import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.idgenerate.core.snowflake.LocalRedisWorkIdChoose;
import me.xgwd.idgenerate.core.snowflake.RandomWorkIdChoose;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/10:38
 * @Description:
 */
@Configuration
@Import(ApplicationContextHolder.class)
public class DistributedIdAutoConfiguration {

    /**
     * 本地 Redis 构建雪花 WorkId 选择器
     */
    @Bean
    @ConditionalOnProperty("spring.data.redis")
    // cluster模式
    public LocalRedisWorkIdChoose redisWorkIdChoose() {
        return new LocalRedisWorkIdChoose();
    }

    /**
     * 随机数构建雪花 WorkId 选择器。如果项目未使用 Redis，使用该选择器
     */
    @Bean
    @ConditionalOnMissingBean(LocalRedisWorkIdChoose.class)
    public RandomWorkIdChoose randomWorkIdChoose() {
        return new RandomWorkIdChoose();
    }
}
