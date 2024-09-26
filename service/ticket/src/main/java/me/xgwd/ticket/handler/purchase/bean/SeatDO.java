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

package me.xgwd.ticket.handler.purchase.bean;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.xgwd.dao.base.BaseDO;

/**
 * 座位实体
 */
@Data
@TableName("t_seat")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 列车id
     */
    @JSONField(name = "train_id")
    private Long trainId;

    /**
     * 车厢号
     */
    @JSONField(name = "carriage_number")
    private String carriageNumber;

    /**
     * 座位号
     */
    @JSONField(name = "seat_number")
    private String seatNumber;

    /**
     * 座位类型
     */
    @JSONField(name = "seat_type")
    private Integer seatType;

    /**
     * 起始站
     */
    @JSONField(name = "start_station")
    private String startStation;

    /**
     * 终点站
     */
    @JSONField(name = "end_station")
    private String endStation;

    /**
     * 座位状态
     */
    @JSONField(name = "seat_status")
    private Integer seatStatus;

    /**
     * 车票价格
     */
    private Integer price;
}
