package me.xgwd.user;

import me.xgwd.idgenerate.toolkit.SnowflakeIdUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/11:28
 * @Description:
 */
@SpringBootApplication
@ComponentScan("me.xgwd")
public class UserApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(UserApplication.class, args);
        System.out.println(SnowflakeIdUtil.nextId());
    }
}
