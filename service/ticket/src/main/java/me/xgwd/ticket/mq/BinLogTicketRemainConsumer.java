package me.xgwd.ticket.mq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.SeatStatusEnum;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.handler.purchase.bean.SeatDO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * 座位锁定后需要扣减route的座位数量，canal分发到kafka然后消费扣减
 */
@Component
@Slf4j
public class BinLogTicketRemainConsumer {

    @Autowired
    private DistributedCache distributedCache;

    @KafkaListener(topics = "12306_topic", groupId = "12306_topic_group")
    public void consumerTicketRemain(ConsumerRecord<?, ?> record) {
        try{
            JSONObject json = JSONObject.parseObject(record.value().toString());
            Object id = json.get("id");
            String type = (String) json.get("type");
            List<SeatDO> data = JSONArray.parseArray(json.get("data").toString(), SeatDO.class);
            String table = (String) json.get("table");
            if (!table.startsWith("t_seat")) {
                return;
            }
            List<Map<String, Object>> olds = (List<Map<String, Object>>)json.get("old");

            List<SeatDO> seatDOListList = new ArrayList<>();
            List<Map<String, Object>> actualOldDataList = new ArrayList<>();
            // 遍历 update下旧数据消息
            for (int i = 0; i < olds.size(); i++) {
                Map<String, Object> oldDataMap = olds.get(i);
                // 如果是修改
                if (oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())) {
                    SeatDO seatDO = data.get(i);
                    // 如果原来的旧数据是available 新数据是lock得话那么加入
                    if (StrUtil.equalsAny(seatDO.getSeatStatus().toString(), String.valueOf(SeatStatusEnum.AVAILABLE.getCode()), String.valueOf(SeatStatusEnum.LOCKED.getCode()))) {
                        // 实际的旧数据
                        actualOldDataList.add(oldDataMap);
                        seatDOListList.add(seatDO);
                    }
                }
            }
            if (CollUtil.isEmpty(seatDOListList) || CollUtil.isEmpty(actualOldDataList)) {
                return;
            }

// 真正更新逻辑
            Map<String, Map<Integer, Integer>> cacheChangeKeyMap = new HashMap<>();
            for (int i = 0; i < seatDOListList.size(); i++) {
                SeatDO seatDO = seatDOListList.get(i);
                Map<String, Object> actualOldData = actualOldDataList.get(i);

                // 取出旧数据的seatStatus
                String seatStatus = actualOldData.get("seat_status").toString();
                // 判断是+1还是-1
                int increment = Objects.equals(seatStatus, "0") ? -1 : 1;
                // 获取trainId
                String trainId = seatDO.getTrainId().toString();
                // 拼接然后+上就行
                String hashCacheKey = RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + trainId + "_" + seatDO.getStartStation() + "_" + seatDO.getEndStation();
                Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.get(hashCacheKey);
                if (CollUtil.isEmpty(seatTypeMap)) {
                    seatTypeMap = new HashMap<>();
                }
                Integer seatType = seatDO.getSeatType();
                Integer num = seatTypeMap.get(seatType);
                seatTypeMap.put(seatType, num == null ? increment : num + increment);
                cacheChangeKeyMap.put(hashCacheKey, seatTypeMap);
            }
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            for (String redisKey : cacheChangeKeyMap.keySet()) {
                Map<Integer, Integer> map = cacheChangeKeyMap.get(redisKey);
                for (Integer seatType : map.keySet()) {
                    String sSeatType = String.valueOf(seatType);
                    System.out.println("更改前" + instance.opsForHash().get(redisKey, sSeatType));
                    instance.opsForHash().increment(redisKey, String.valueOf(seatType), map.get(seatType));
                    System.out.println("更改后" + instance.opsForHash().get(redisKey, sSeatType));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
