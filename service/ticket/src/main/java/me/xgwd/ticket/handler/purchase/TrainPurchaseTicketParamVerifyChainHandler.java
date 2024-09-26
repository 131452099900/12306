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

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import me.xgwd.base.exception.ClientException;
import me.xgwd.bean.dto.PurchaseTicketReqDTO;

import me.xgwd.cache.DistributedCache;
import me.xgwd.common.util.EnvironmentUtil;
import me.xgwd.ticket.common.RedisKeyConstant;
import me.xgwd.ticket.handler.purchase.bean.TrainDO;
import me.xgwd.ticket.handler.purchase.bean.TrainStationDO;
import me.xgwd.ticket.mapper.TrainMapper;
import me.xgwd.ticket.mapper.TrainStationMapper;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * 购票流程过滤器之验证参数是否有效
 * 验证参数有效这个流程会大量交互缓存，为了优化性能需要使用 Lua。为了方便大家理解流程，这里使用多次调用缓存
 * * 查询 trainDO 缓存 查询了两次缓存而已 一共4次操作redis
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamVerifyChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    private final TrainMapper trainMapper;
    private final TrainStationMapper trainStationMapper;
    private final DistributedCache distributedCache;


    // 在缓存钟获取车次 车次是否在售 车站顺序是否正确
    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 查询会员购票车次是否存在，通过封装后安全的 Get 方法
        TrainDO trainDO = distributedCache.safeGet(
                RedisKeyConstant.TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()), // 如果没有则查询后在放入缓存
                15, // 过期时间 也就是 可以提前购买的天数
                TimeUnit.DAYS);

        if (Objects.isNull(trainDO)) {
            // 如果按照严谨逻辑，类似异常应该记录当前用户的 userid 并发送到风控中心
            // 如果一段时间有过几次的异常，直接封号处理。下述异常同理
            throw new ClientException("请检查车次是否存在");
        }

        // TODO，当前列车数据并没有通过定时任务每天生成最新的，所以需要隔离这个拦截。后期定时生成数据后删除该判断
        if (!EnvironmentUtil.isDevEnvironment()) {
            // 查询车次是否已经发售
            if (new Date().before(trainDO.getSaleTime())) {
                throw new ClientException("列车车次暂未发售");
            }
            // 查询车次是否在有效期内
            if (new Date().after(trainDO.getDepartureTime())) {
                throw new ClientException("列车车次已出发禁止购票");
            }
        }


        // 车站是否存在车次中，以及车站的顺序是否正确
        String trainStationStopoverDetailStr = distributedCache.safeGet(
                RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL + requestParam.getTrainId(),
                String.class,
                () -> {
                    // 当第一次查询这个车次的这个旅程会装载进去
                    LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                            .eq(TrainStationDO::getTrainId, requestParam.getTrainId())
                            .select(TrainStationDO::getDeparture);
                    // 把对应车次的所有站点查询出来 放到缓存钟
                    List<TrainStationDO> actualTrainStationList = trainStationMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(actualTrainStationList) ? JSON.toJSONString(actualTrainStationList) : null;
                },
                15,
                TimeUnit.DAYS
        );

        // 获取参数的出发站点和终止比较
        // 北京 德州 嘉兴 海宁 杭州
        List<TrainStationDO> trainDOList = JSON.parseArray(trainStationStopoverDetailStr, TrainStationDO.class);
        boolean validateStation = validateStation(
                trainDOList.stream().map(TrainStationDO::getDeparture).toList(),
                requestParam.getDeparture(),
                requestParam.getArrival()
        );
        if (!validateStation) {
            throw new ClientException("列车车站数据错误");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }

    public boolean validateStation(List<String> stationList, String startStation, String endStation) {
        int index1 = stationList.indexOf(startStation);
        int index2 = stationList.indexOf(endStation);
        if (index1 == -1 || index2 == -1) {
            return false;
        }
        return index2 >= index1;
    }
}
