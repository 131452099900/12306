package me.xgwd.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.user.PassengerService;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.exception.ClientException;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.bean.dto.PassengerActualRespDTO;
import me.xgwd.bean.dto.PassengerRemoveReqDTO;
import me.xgwd.bean.dto.PassengerReqDTO;
import me.xgwd.bean.dto.PassengerRespDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.RedisConstant;
import me.xgwd.common.enums.VerifyStatusEnum;
import me.xgwd.user.dao.entity.PassengerDO;
import me.xgwd.user.dao.mapper.PassengerMapper;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/14:13
 * @Description:
 */

@DubboService
@Slf4j
public class PassengerServiceImpl implements PassengerService {

    private final DistributedCache distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);

    private final PassengerMapper passengerMapper = ApplicationContextHolder.getBean(PassengerMapper.class);


    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(item -> JSON.parseArray(item, PassengerDO.class))
                .map(passengerDo -> BeanUtil.copyToList(passengerDo, PassengerRespDTO.class)).orElse(new ArrayList<>());
    }

    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(item -> JSON.parseArray(item, PassengerDO.class)
                        .stream().filter(passengerDO -> ids.contains(passengerDO.getId()))
                        .map(passengerDO -> BeanUtil.toBean(passengerDO, PassengerActualRespDTO.class))
                        .collect(Collectors.toList())).orElse(new ArrayList<>());
    }

    @Override
    public void savePassenger(PassengerReqDTO requestParam, String username) {
        try {
            PassengerDO passengerDO = BeanUtil.toBean(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            int inserted = passengerMapper.insert(passengerDO);
            if (!SqlHelper.retBool(inserted)) {
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
            }
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        // 删除缓存
        delUserPassengerCache(username);
    }

    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        String username = requestParam.getUsername();
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        if (Objects.isNull(passengerDO)) {
            throw new ClientException("乘车人数据不存在");
        }
        try {
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 逻辑删除，修改数据库表记录 del_flag
            int deleted = passengerMapper.delete(deleteWrapper);
            if (!SqlHelper.retBool(deleted)) {
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
            }
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        delUserPassengerCache(username);
    }

    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        String username = requestParam.getUsername();
        try {
            PassengerDO passengerDO = BeanUtil.toBean(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            int updated = passengerMapper.update(passengerDO, updateWrapper);
            if (!SqlHelper.retBool(updated)) {
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        delUserPassengerCache(username);
    }
    private String getActualUserPassengerListStr(String username) {
        return distributedCache.safeGet(
                RedisConstant.USER_PASSENGER_LIST + username,
                String.class,
                () -> {
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },
                1,
                TimeUnit.DAYS
        );
    }

    private void delUserPassengerCache(String username) {
        distributedCache.delete(RedisConstant.USER_PASSENGER_LIST + username);
    }

    private PassengerDO selectPassenger(String username, String passengerId) {
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        return passengerMapper.selectOne(queryWrapper);
    }

}
