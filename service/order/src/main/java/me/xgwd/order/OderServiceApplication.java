package me.xgwd.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/23:36
 * @Description:
 */
@SpringBootApplication
@EnableDubbo
@ComponentScan("me.xgwd")
public class OderServiceApplication{
    public static void main(String[] args) {
        SpringApplication.run(OderServiceApplication.class, args);

    }
}
