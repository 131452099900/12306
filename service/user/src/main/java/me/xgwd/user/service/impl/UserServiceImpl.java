package me.xgwd.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.user.UserService;
import me.xgwd.auth.core.UserContext;
import me.xgwd.auth.core.UserInfoDTO;
import me.xgwd.auth.util.JWTUtil;
import me.xgwd.base.ApplicationContextHolder;
import me.xgwd.base.exception.AbstractException;
import me.xgwd.base.exception.ClientException;
import me.xgwd.base.exception.ServiceException;
import me.xgwd.base.resp.Result;
import me.xgwd.bean.dto.*;
import me.xgwd.cache.DistributedCache;
import me.xgwd.common.enums.RedisConstant;
import me.xgwd.common.enums.UserRegisterErrorCodeEnum;
import me.xgwd.common.util.UserReuseUtil;
import me.xgwd.user.dao.entity.*;
import me.xgwd.user.dao.mapper.*;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
@DubboService(filter = "dubboRpcFilter")
@Slf4j
public class UserServiceImpl implements UserService{
    private final RBloomFilter userRegisterCachePenetrationBloomFilter = ApplicationContextHolder.getBean(RBloomFilter.class);
    private final DistributedCache distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
    private final UserDeletionDOService userDeletionDOService = ApplicationContextHolder.getBean(UserDeletionDOService.class);
    private final RedissonClient redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
    private final UserPhoneMapper phoneMapper = ApplicationContextHolder.getBean(UserPhoneMapper.class);
    private final UserMailMapper userMailMapper = ApplicationContextHolder.getBean(UserMailMapper.class);
    private final UserReuseMapper userReuseMapper = ApplicationContextHolder.getBean(UserReuseMapper.class);
    private final UserMapper userMapper = ApplicationContextHolder.getBean(UserMapper.class);
    private final UserDeletionMapper userDeletionMapper = ApplicationContextHolder.getBean(UserDeletionMapper.class);

    @Override
    public void test() {
        System.out.println("test");
    }

    @Override
    public Result<UserRegisterRespDTO> register(UserRegisterReqDTO requestParam) throws AbstractException {
        // 1. 非空校验 布隆过滤器 redis的set注销判断 黑名单判断
        check(requestParam);

        // 2.获取用户注册锁 有可能是有别的用户在注册这个名称中 或者前面有人注册失败的？
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
                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
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
                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
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
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(UserRegisterErrorCodeEnum.MAIL_REGISTERED);
                }
            }
            // 布隆过滤器
            String username = requestParam.getUsername();
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();

            // 如果有注销过的需要把他给移除掉
            instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
            // 布隆过滤器设计问题：设置多大、碰撞率以及初始容量不够了怎么办？详情查看：https://nageoffer.com/12306/question
            userRegisterCachePenetrationBloomFilter.add(username);
        } finally {
            lock.unlock();
        }
        return Results.success(BeanUtil.toBean(requestParam, UserRegisterRespDTO.class));
    }

    @Override
    public Result<UserLoginRespDTO> login(UserLoginReqDTO requestParam) {
        // 支持邮箱 用户名 手机登录 所以2个额外的路由表 记录手机用户，邮箱用户名关系
        String usernameOrMailOrPhone = requestParam.getUsernameOrMailOrPhone();
        boolean mailFlag = false;
        // 时间复杂度最佳 O(1)。indexOf or contains 时间复杂度为 O(n)
        for (char c : usernameOrMailOrPhone.toCharArray()) {
            // 如果有@则是邮箱 可以用真正来判断
            if (c == '@') {
                mailFlag = true;
                break;
            }
        }
        String username;
        if (mailFlag) {
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrMailOrPhone);
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        } else {

            // 用手机号码查询
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);
            username = Optional.ofNullable(phoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)
                    .orElse(null);
        }

        // 如果用户名为null则是根据用户名查找，不是null那么就从路由表检索出来了
        username = Optional.ofNullable(username).orElse(requestParam.getUsernameOrMailOrPhone());
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, requestParam.getPassword())
                .select(UserDO::getId, UserDO::getUsername, UserDO::getRealName);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        System.out.println(userDO);
        if (userDO != null) {
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();

            // 办法jwt token
            String accessToken = JWTUtil.generateAccessToken(userInfo);
            UserLoginRespDTO actual = new UserLoginRespDTO(userInfo.getUserId(), requestParam.getUsernameOrMailOrPhone(), userDO.getRealName(), accessToken);

            // 把用户信息放到缓存中 中要是防止退出登录有jwt还能使用
            log.info("user login success token is {}", accessToken);
            distributedCache.put(accessToken, JSON.toJSONString(actual), 30, TimeUnit.MINUTES);
            return Results.success(actual);
        }
        throw new ServiceException("账号不存在或密码错误");
    }


    @Override
    public Result<UserLoginRespDTO> checkLogin(String accessToken) {
        UserLoginRespDTO userLoginRespDTO = distributedCache.get(accessToken, UserLoginRespDTO.class);
        return Results.success(userLoginRespDTO);
    }

    @Override
    public Result<UserQueryRespDTO> queryUserByUsername(String username) {
        UserQueryRespDTO respDTO = BeanUtil.toBean(queryRespDTO(username), UserQueryRespDTO.class);
        return Results.success(respDTO);
    }

    @Override
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(String username) {
        UserDO userDO = queryRespDTO(username);
        return Results.success(BeanUtil.toBean(userDO, UserQueryActualRespDTO.class));
    }

    public UserDO queryRespDTO(String username) {
        if (StringUtils.isBlank(username)) throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(wrapper);
        if (Objects.isNull(userDO)) throw new ServiceException(UserRegisterErrorCodeEnum.USER_NOT_EXISIT);
        return userDO;
    }

    @Override
    public Result<Boolean> hasUsername(String username) {
        if (StringUtils.isBlank(username)) throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if (hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            Boolean member = instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
            // 如果set存在则是已经被注销了的，不存在则是
            return Results.success(!member);
        }
        return Results.success(Boolean.TRUE);
    }

    @Override
    public Result<Void> update(UserUpdateReqDTO requestParam) {
        // 不提供手机修改
        UserDO userDO = queryRespDTO(requestParam.getUsername());
        UserDO userDO1 = BeanUtil.toBean(requestParam, UserDO.class);
        // 修改user
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        userMapper.update(userDO1, userUpdateWrapper);

        // 修改mail
        if (StrUtil.isNotBlank(requestParam.getMail()) && !Objects.equals(requestParam.getMail(), userDO.getMail())) {
            LambdaUpdateWrapper<UserMailDO> updateWrapper = Wrappers.lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userDO.getMail());
            userMailMapper.delete(updateWrapper);
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            userMailMapper.insert(userMailDO);
        }
        return Results.success();
    }

    @Override
    public Result<Void> deletion(UserDeletionReqDTO requestParam) {
        String username = requestParam.getUsername();
        RLock lock = redissonClient.getLock(RedisConstant.USER_DELETION + requestParam.getUsername());
        lock.lock();
        try {
            UserDO userDO = queryRespDTO(username);

            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userDO.getIdType())
                    .idCard(userDO.getIdCard())
                    .build();

            // 插入注销表
            userDeletionMapper.insert(userDeletionDO);

            // user表逻辑删除
            UserDO userDO1 = new UserDO();
            userDO1.setDeletionTime(System.currentTimeMillis());
            userDO1.setUsername(username);
            LambdaUpdateWrapper<UserDO> userWrapper = Wrappers.lambdaUpdate(UserDO.class).eq(UserDO::getUsername, username);
            userMapper.delete(userWrapper);

            // phone表逻辑删除
            if (StringUtils.isNotBlank(userDO.getPhone())) {
                UserPhoneDO userPhoneDO = new UserPhoneDO();
                userPhoneDO.setDeletionTime(System.currentTimeMillis());
                userPhoneDO.setPhone(userDO.getPhone());
                LambdaUpdateWrapper<UserPhoneDO> userPhoneWrapper = Wrappers.lambdaUpdate(UserPhoneDO.class).eq(UserPhoneDO::getPhone, userDO.getPhone());
                phoneMapper.delete( userPhoneWrapper);
            }

            // mail表逻辑删除
            if (StringUtils.isNotBlank(userDO.getMail())) {
                UserMailDO userMailDO = new UserMailDO();
                userMailDO.setDeletionTime(System.currentTimeMillis());
                userMailDO.setMail(userDO.getMail());
                LambdaUpdateWrapper<UserMailDO> mailWrapper = Wrappers.lambdaUpdate(UserMailDO.class).eq(UserMailDO::getMail, userDO.getMail());
                userMailMapper.delete(mailWrapper);
            }

            // 滥用表逻辑删除
            UserReuseDO userReuseDO = new UserReuseDO(username);
            userReuseMapper.insert(userReuseDO);

            // 删除redis token
            distributedCache.delete(UserContext.getToken());

            // 无法删除布隆过滤器 只能在set加
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        } finally {
            lock.unlock();
        }
        return Results.success();
    }

    @Override
    public Result<Void> logout(String accessToken) {
        distributedCache.delete(UserContext.getToken());
        return Results.success();
    }


    @Transactional(rollbackFor = Exception.class)
    public void check(UserRegisterReqDTO requestParam){
        String username = requestParam.getUsername();
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
            // 如果不存在说明真的有这个用户名了 如果存在说明已经注销可以注册
            if (!member) throw new ClientException("用户名已经存在并且注销");
        }

        // 通过身份证查看注销了多少次，次数过多放入黑名单
        Integer num = userDeletionDOService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        if (num > 5) throw new ClientException("证件号多次注销账号已被加入黑名单");
    }

}
