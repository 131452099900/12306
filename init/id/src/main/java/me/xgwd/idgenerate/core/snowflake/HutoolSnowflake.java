package me.xgwd.idgenerate.core.snowflake;

import cn.hutool.core.util.IdUtil;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/0:26
 * @Description:
 */
public class HutoolSnowflake extends Snowflake{
    cn.hutool.core.lang.Snowflake snowflake;
    public static void main(String[] args) {
        new cn.hutool.core.lang.Snowflake();
    }
    public HutoolSnowflake(long workId, long dataCentorId) {
        snowflake = IdUtil.getSnowflake(workId, dataCentorId);
    }

    @Override
    public long getWorkerId(long id) {
        return snowflake.getWorkerId(id);
    }

    @Override
    public long getDataCenterId(long id) {
        return snowflake.getDataCenterId(id);
    }

    @Override
    public long getGenerateDateTime(long id) {
        return snowflake.getGenerateDateTime(id);
    }

    @Override
    public synchronized long nextId() {
        return snowflake.nextId();
    }

    @Override
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }

}
