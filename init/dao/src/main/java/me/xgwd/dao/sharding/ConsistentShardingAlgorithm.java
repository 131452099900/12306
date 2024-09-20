package me.xgwd.dao.sharding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/09/11:04
 * @Description:
 */
// 实现了两种算法 PreciseShardingAlgorithm（精准分片算法）、RangeShardingAlgorithm（范围分片算法）
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsistentShardingAlgorithm
        implements PreciseShardingAlgorithm<String>, RangeShardingAlgorithm<String> {



    /**
     * 精确分片
     * 一致性hash算法
     */
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        int i = preciseShardingValue.getValue().hashCode();

        ArrayList<String> availableTargetNameList = new ArrayList<>(collection);
        int index = i % availableTargetNameList.size();
        System.out.println("hashCode is " + i + " and Index is " + index);
        return availableTargetNameList.get(index);
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<String> rangeShardingValue) {
        return collection;
    }

}
