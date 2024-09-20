package me.xgwd.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.xgwd.user.dao.entity.UserDeletionDO;
import me.xgwd.user.dao.mapper.UserDeletionMapper;
import me.xgwd.user.service.UserDeletionDOService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/17:52
 * @Description:
 */
@Service
public class UserDeletionDOServiceImpl extends ServiceImpl<UserDeletionMapper, UserDeletionDO> implements IService<UserDeletionDO>, UserDeletionDOService {

    @Override
    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        // TODO 此处应该先查缓存
        Long deletionCount = lambdaQuery().eq(UserDeletionDO::getIdType, idType)
                .eq(UserDeletionDO::getIdCard, idCard)
                .count();
        return Optional.ofNullable(deletionCount).map(Long::intValue).orElse(0);
    }
}
