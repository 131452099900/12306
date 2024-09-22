/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain logback-spring.xml copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xgwd.user.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.xgwd.user.dao.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息持久层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
//    /**
//     * 注销用户
//     *
//     * @param userDO 注销用户入参
//     */
//    void deletionUser(UserDO userDO);
}
