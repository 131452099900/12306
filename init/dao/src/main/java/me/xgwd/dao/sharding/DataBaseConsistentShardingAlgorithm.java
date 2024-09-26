package me.xgwd.dao.sharding;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
@Component
public class DataBaseConsistentShardingAlgorithm implements PreciseShardingAlgorithm<String>, RangeShardingAlgorithm<Long> {


    public static final int shardingCount = 32;
    public static final int tableShardingCount = 16;

    // 简单路由分片 不满16为0，满16为1
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        int tableIndex = hashShardingValue(preciseShardingValue.getValue()) % shardingCount;
        // 0-15在 0  16-31在1
        int index = tableIndex / tableShardingCount;
        System.out.println("value is " + preciseShardingValue.getValue() + "hashCode is " + index + " and Index is " + index + " db");

        return new ArrayList<>(collection).get(index);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return collection;
    }

    private int hashShardingValue(final Object shardingValue) {
        return Math.abs( shardingValue.hashCode());
    }
}
