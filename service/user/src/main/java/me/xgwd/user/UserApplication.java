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
        DistributedCache bean = run.getBean(DistributedCache.class);
        bean.put("test_key", "value", 100, TimeUnit.MINUTES);
        bean.put("test_key", "value");
        String test_key = bean.get("test_key", String.class);
        StringRedisTemplate template = run.getBean(StringRedisTemplate.class);
        template.opsForValue().set("keye", "v");
        System.out.println(test_key);
        System.out.println(bean.get("Bearer eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE3MjY5MzEyNjQsImlzcyI6ImluZGV4MTIzMDYiLCJzdWIiOiJ7XCJyZWFsTmFtZVwiOlwi6buE5a626L6JXCIsXCJ1c2VySWRcIjpcIjE4Mzc1MDMzMDI3MDI0NDA0NDhcIixcInVzZXJuYW1lXCI6XCJnYmwyZS4xMzEyMzEzXCJ9IiwiZXhwIjoxNzI3MDE3NjY0fQ.abIAwc3_nsfG0bxbV-E7Mpmx1UrKXqvQs-LMparrxAPUMknPzcjW5qpzNAVidiOi8OJXTvS_XoSKu4xe2L1dCw", String.class));
    }
}
