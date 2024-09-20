package me.xgwd.user.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.xgwd.api.user.UserService;
import me.xgwd.user.dao.entity.UserDO;
import me.xgwd.user.dao.entity.UserPhoneDO;
import me.xgwd.user.dao.mapper.UserMapper;
import me.xgwd.user.dao.mapper.UserPhoneMapper;
import me.xgwd.user.service.UserPhoneService;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/20/18:18
 * @Description:
 */
@Service
public class UserPhoneServiceImpl extends ServiceImpl<UserPhoneMapper, UserPhoneDO> implements IService<UserPhoneDO>, UserPhoneService {
}
