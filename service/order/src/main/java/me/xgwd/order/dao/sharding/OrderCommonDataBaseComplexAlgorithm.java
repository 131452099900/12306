package me.xgwd.order.dao.sharding;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/22/21:28
 * @Description:
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {
//    @Getter
//    private Properties props;

    // 分库数量，读取的配置中定义的分库数量
    private int shardingCount = 32;
    private int tableShardingCount = 16;
    private static final String SHARDING_COUNT_KEY = "sharding-count";
    public static final String USER_ID_FILED = "user_id";
    public static final String ORDER_SN_FILED = "order_sn";
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
            String dbSuffix;
            if (comparable instanceof String) {
                String actualOrderSn = comparable.toString();
                dbSuffix = String.valueOf(hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0))) % shardingCount / tableShardingCount);
            } else {
                dbSuffix = String.valueOf(hashShardingValue((Long) comparable % 1000000) % shardingCount / tableShardingCount);
            }
            result.add("ds_" + dbSuffix);
        }
        // 返回的是表名，
        return result;
    }


    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}
