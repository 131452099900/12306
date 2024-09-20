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

    // 简单路由分片
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        int i = preciseShardingValue.getValue().hashCode();
        ArrayList<String> availableTargetNameList = new ArrayList<>(collection);
        int index = i % availableTargetNameList.size();
        return availableTargetNameList.get(index);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return collection;
    }
}
