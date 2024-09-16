package me.xgwd.dao.handle;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import me.xgwd.idgenerate.toolkit.SnowflakeIdUtil;

/**
 * 使用雪花ID
 *
 */
public class CustomIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return SnowflakeIdUtil.nextId();
    }
}
