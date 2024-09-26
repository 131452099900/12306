package me.xgwd.user;

import me.xgwd.cache.DistributedCache;
import me.xgwd.user.dao.entity.UserDO;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/11:28
 * @Description:
 */
@EnableDubbo
@SpringBootApplication
@ComponentScan("me.xgwd.*")
public class UserApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(UserApplication.class, args);
    }
}
