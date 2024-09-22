package me.xgwd.order.dao.sharding;

import cn.hutool.core.collection.CollUtil;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/21:30
 * @Description:
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {
    public static final String USER_ID_FILED = "user_id";
    public static final String ORDER_SN_FILED = "order_sn";
    private int shardingCount;
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        Map<String, Collection<Comparable<Long>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());


        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            // 首先判断 SQL 是否包含用户 ID，如果包含直接取用户 ID 后六位
            Collection<Comparable<Long>> customerUserIdCollection = columnNameAndShardingValuesMap.get(USER_ID_FILED);
            if (customerUserIdCollection.isEmpty()) {
                customerUserIdCollection = columnNameAndShardingValuesMap.get(ORDER_SN_FILED);
            }
            Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();
            if (comparable instanceof String) {
                String actualUserId = comparable.toString();
                result.add(shardingValue.getLogicTableName() + "_" + hashShardingValue(actualUserId.substring(Math.max(actualUserId.length() - 6, 0))) % shardingCount);
            } else {
                String dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount);
                result.add(shardingValue.getLogicTableName() + "_" + dbSuffix);
            }
        }
        // 返回的是表名，
        return result;
    }

    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}
