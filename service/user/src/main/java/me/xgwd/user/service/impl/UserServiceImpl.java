package me.xgwd.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.user.UserService;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.exception.ClientException;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.UserRegisterReqDTO;
import me.xgwd.bean.dto.UserRegisterRespDTO;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.RedisConstant;
import me.xgwd.common.enums.UserRegisterErrorCodeEnum;
import me.xgwd.common.util.UserReuseUtil;
import me.xgwd.user.dao.entity.UserDO;
import me.xgwd.user.dao.entity.UserMailDO;
import me.xgwd.user.dao.entity.UserPhoneDO;
import me.xgwd.user.dao.entity.UserReuseDO;
import me.xgwd.user.dao.mapper.UserMailMapper;
import me.xgwd.user.dao.mapper.UserMapper;
import me.xgwd.user.dao.mapper.UserPhoneMapper;
import me.xgwd.user.dao.mapper.UserReuseMapper;
import me.xgwd.user.service.UserDeletionDOService;
import me.xgwd.user.service.UserPhoneService;
import me.xgwd.web.res.Results;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

import static me.xgwd.common.enums.RedisConstant.USER_REGISTER_REUSE_SHARDING;
import static me.xgwd.common.util.UserReuseUtil.hashShardingIdx;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/19/22:12
 * @Description:
 */
//@Component
@DubboService
public class UserServiceImpl implements UserService{
    private final RBloomFilter userRegisterCachePenetrationBloomFilter = ApplicationContextHolder.getBean(RBloomFilter.class);
    private final DistributedCache distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
    private final UserDeletionDOService userDeletionDOService = ApplicationContextHolder.getBean(UserDeletionDOService.class);
    private final RedissonClient redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
    private final UserPhoneMapper phoneMapper = ApplicationContextHolder.getBean(UserPhoneMapper.class);
    private final UserMailMapper userMailMapper = ApplicationContextHolder.getBean(UserMailMapper.class);
    private final UserReuseMapper userReuseMapper = ApplicationContextHolder.getBean(UserReuseMapper.class);
    private final UserMapper userMapper = ApplicationContextHolder.getBean(UserMapper.class);
    @Override
    public void test() {
        System.out.println("test");
    }

    @Override
    public Result<UserRegisterRespDTO> register(UserRegisterReqDTO requestParam) {

        // 1. 非空校验 布隆过滤器 redis的set注销判断 黑名单判断
        check(requestParam);

        // 2.获取用户注册锁 有可能是有别的用户在注册这个名称中
        RLock lock = redissonClient.getLock(RedisConstant.LOCK_USER_REGISTER + requestParam.getUsername());
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ServiceException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }

        try {
            try {
                // 3.插入用户表
                if (userMapper.insert(BeanUtil.toBean(requestParam, UserDO.class)) < 1) {
                    throw new ServiceException(UserRegisterErrorCodeEnum.USER_REGISTER_FAIL);
                }

            } catch (DuplicateKeyException dke) {
//                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
                throw new ServiceException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
            }

            // 4.插入用户手机表
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(requestParam.getPhone())
                    .username(requestParam.getUsername())
                    .build();
            try {
                int insert = phoneMapper.insert(userPhoneDO);
            } catch (DuplicateKeyException dke) {
//                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
                throw new ServiceException(UserRegisterErrorCodeEnum.PHONE_REGISTERED);
            }

            // 5.插入用户邮箱表
            if (StrUtil.isNotBlank(requestParam.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(requestParam.getMail())
                        .username(requestParam.getUsername())
                        .build();
                try {
                    userMailMapper.insert(userMailDO);
                } catch (DuplicateKeyException dke) {
//                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(UserRegisterErrorCodeEnum.MAIL_REGISTERED);
                }
            }
            // 布隆过滤器
            String username = requestParam.getUsername();
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
            // 布隆过滤器设计问题：设置多大、碰撞率以及初始容量不够了怎么办？详情查看：https://nageoffer.com/12306/question
            userRegisterCachePenetrationBloomFilter.add(username);
        } finally {
            lock.unlock();
        }
        return Results.success(BeanUtil.toBean(requestParam, UserRegisterRespDTO.class));
    }



    public void check(UserRegisterReqDTO requestParam){
        String username = requestParam.getUsername();
        System.out.println(userRegisterCachePenetrationBloomFilter);
         // 校验参数
         if (Objects.isNull(requestParam.getUsername())) {
             throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
         } else if (Objects.isNull(requestParam.getPassword())) {
             throw new ClientException(UserRegisterErrorCodeEnum.PASSWORD_NOTNULL);
         } else if (Objects.isNull(requestParam.getPhone())) {
             throw new ClientException(UserRegisterErrorCodeEnum.PHONE_NOTNULL);
         } else if (Objects.isNull(requestParam.getIdType())) {
             throw new ClientException(UserRegisterErrorCodeEnum.ID_TYPE_NOTNULL);
         } else if (Objects.isNull(requestParam.getIdCard())) {
             throw new ClientException(UserRegisterErrorCodeEnum.ID_CARD_NOTNULL);
         }

         // 布隆过滤器校验 校验是否已经注销 可能布隆过滤器存在，但是set注销
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if (hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            Boolean member = instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
            if (member) throw new ClientException("用户名已经存在并且注销");
        }

        // 通过身份证查看注销了多少次，次数过多放入黑名单
        Integer num = userDeletionDOService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        if (num > 5) throw new ClientException("证件号多次注销账号已被加入黑名单");


    }
}
