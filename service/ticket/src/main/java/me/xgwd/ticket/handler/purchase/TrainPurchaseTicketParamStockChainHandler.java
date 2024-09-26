/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xgwd.ticket.handler.purchase;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import me.xgwd.base.exception.ClientException;
import me.xgwd.bean.doain.PurchaseTicketPassengerDetailDTO;
import me.xgwd.bean.dto.PurchaseTicketReqDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.sservice.SeatMarginCacheLoader;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 购票流程过滤器之验证列车站点库存是否充足
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamStockChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final DistributedCache distributedCache;

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // requestParam为乘车人 选座 出发-结束站点

        // 对座位分组，锁的粒度修改
        // 0 [passegerID:1837751431885856768,seatType:0]
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypePassengersMap = requestParam.getPassengers()
                .stream().collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));

        // 把车次和出发站点，结束站点拼接起来 3_德州_海宁
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(),
                requestParam.getDeparture(), requestParam.getArrival());

        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // *** 计算每个座位类型剩余的余票是否满足乘车人数量需求
        for (Integer seatType : seatTypePassengersMap.keySet()) {
            // 对每个车次获取余票 (3_德州_海宁 3) 的余票没有就加载
            // TODO 后面得修改成某个站点的价格
            Object stockObj = stringRedisTemplate.opsForHash().get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType));

            Integer stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                // 获取到 3_德州_海宁 的4中座位数量
                Map<String, String> seatMarginMap = seatMarginCacheLoader
                        .load(requestParam.getTrainId(), seatType.toString(), requestParam.getDeparture(), requestParam.getArrival());


                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt).orElse(0);
            });

            // 余票充足
            if (stock >= seatTypePassengersMap.get(seatType).size()) {
                continue;
            }

            throw new ClientException("列车站点已无余票");
        }
    }

//    @Override
    public void handler1(PurchaseTicketReqDTO requestParam) {
        // 车次站点是否还有余票。如果用户提交多个乘车人非同一座位类型，拆分验证

        // 把车次和出发站点，结束站点拼接起来 3_德州_海宁
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());

        // 获取到本旅程所有乘车人（出发点-结束点）
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();

        // 对座位分组，锁的粒度修改
        // 0 [passegerID:1837751431885856768,seatType:0]
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));


        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            // 对每个车次获取余票 (3_德州_海宁 0) 的余票没有就加载
            Object stockObj = stringRedisTemplate.opsForHash().get(RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType));

            // 转换，如果是null就重seatMarginCacheLoader加载（）
            int stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                // 如果从缓存中获取到的是null就加载车票
                Map<String, String> seatMarginMap = seatMarginCacheLoader.load(String.valueOf(requestParam.getTrainId()), String.valueOf(seatType), requestParam.getDeparture(), requestParam.getArrival());
                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt).orElse(0);
            });

            // 余票充足
            if (stock >= passengerSeatDetails.size()) {
                return;
            }
            throw new ClientException("列车站点已无余票");
        });
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
