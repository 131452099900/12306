package me.xgwd.ticket;

import me.xgwd.cache.DistributedCache;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/24/9:41
 * @Description:
 */
@SpringBootApplication
@ComponentScan("me.xgwd")
@EnableDubbo
public class TicketConsumerApplication {
    static String qq = "local inputString = KEYS[2]\n" +
            "local actualKey = inputString\n" +
            "local colonIndex = string.find(actualKey, \":\")\n" +
            "if colonIndex ~= nil then\n" +
            "        actualKey = \"{\" .. string.sub(actualKey, colonIndex + 1) .. \"}\"\n" +
            "end\n" +
            "\n" +
            "local jsonArrayStr = ARGV[1]\n" +
            "local jsonArray = cjson.decode(jsonArrayStr)\n" +
            "\n" +
            "local result = {}\n" +
            "local tokenIsNull = false\n" +
            "local tokenIsNullSeatTypeCounts = {}\n" +
            "\n" +
            "for index, jsonObj in ipairs(jsonArray) do\n" +
            "    local seatType = tonumber(jsonObj.seatType)\n" +
            "    local count = tonumber(jsonObj.count)\n" +
            "    local actualInnerHashKey = actualKey .. \"_\" .. seatType\n" +
            "    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))\n" +
            "    if ticketSeatAvailabilityTokenValue < count then\n" +
            "        tokenIsNull = true\n" +
            "        table.insert(tokenIsNullSeatTypeCounts, seatType .. \"_\" .. count)\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "return result";
    static String s = "local inputString = KEYS[2]\n" +
            "local actualKey = inputString\n" +
            "local colonIndex = string.find(actualKey, \":\")\n" +
            "if colonIndex ~= nil then\n" +
            "    actualKey =  string.sub(actualKey, colonIndex + 1)\n" +
            "end\n" +
            "\n" +
            "local jsonArrayStr = ARGV[1]\n" +
            "local jsonArray = cjson.decode(jsonArrayStr)\n" +
            "\n" +
            "local result = {}\n" +
            "local tokenIsNull = false\n" +
            "local tokenIsNullSeatTypeCounts = {}\n" +
            "\n" +
            "for index, jsonObj in ipairs(jsonArray) do\n" +
            "    local seatType = tonumber(jsonObj.seatType)\n" +
            "    local count = tonumber(jsonObj.count)\n" +
            "    local actualInnerHashKey = actualKey .. \"_\" .. seatType\n" +
            "    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))\n" +
            "    if ticketSeatAvailabilityTokenValue < count then\n" +
            "        tokenIsNull = true\n" +
            "        table.insert(tokenIsNullSeatTypeCounts, seatType .. \"_\" .. count)\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "result['tokenIsNull'] = tokenIsNull\n" +
            "if tokenIsNull then\n" +
            "    result['tokenIsNullSeatTypeCounts'] = tokenIsNullSeatTypeCounts\n" +
            "    return cjson.encode(result)\n" +
            "end\n" +
            "\n" +
            "local alongJsonArrayStr = ARGV[2]\n" +
            "local alongJsonArray = cjson.decode(alongJsonArrayStr)\n" +
            "\n" +
            "for index, jsonObj in ipairs(jsonArray) do\n" +
            "    local seatType = tonumber(jsonObj.seatType)\n" +
            "    local count = tonumber(jsonObj.count)\n" +
            "    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do\n" +
            "        local startStation = tostring(alongJsonObj.startStation)\n" +
            "        local endStation = tostring(alongJsonObj.endStation)\n" +
            "        local actualInnerHashKey = startStation .. \"_\" .. endStation .. \"_\" .. seatType\n" +
            "        redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "return cjson.encode(result)\n";

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(TicketConsumerApplication.class, args);
        DistributedCache bean = run.getBean(DistributedCache.class);
        StringRedisTemplate instance =(StringRedisTemplate) bean.getInstance();
        String tokenBucketHashKey = "ticket-service:ticket_availability_token_bucket:1";
        String luaScriptKey = "济南西_宁波_1";
        System.out.println(instance.opsForHash().get(tokenBucketHashKey, luaScriptKey));
    }

    @Autowired
    private DistributedCache distributedCache;
    public void wq() {
        StringRedisTemplate instance =(StringRedisTemplate) distributedCache.getInstance();
        System.out.println(instance.opsForHash().get("ticket-service:ticket_availability_token_bucket:{3}", "德州_海宁_3"));
    }
}
