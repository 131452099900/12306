package me.xgwd.ticket;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.apache.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration;
/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/23/14:43
 * @Description:
 */
@EnableDubbo
@SpringBootApplication(exclude = SpringBootConfiguration.class)
@ComponentScan("me.xgwd.*")
//@MapperScan("me.xgwd.ticket.mapper")
public class TicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
