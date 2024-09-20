//package me.xgwd.dao.sharding;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @Author:
// * @Date: 2024/09/18/0:28
// * @Description:
// */
//
//import com.baomidou.mybatisplus.core.toolkit.StringUtils;
//import jakarta.annotation.Resource;
//import lombok.Data;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.shardingsphere.core.rule.ShardingRule;
//import org.apache.shardingsphere.core.rule.TableRule;
//import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
//import org.apache.shardingsphere.underlying.common.rule.DataNode;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @Author:
// * @Date: 2024/09/17/23:03
// * @Description:
// */
//@Component
//@Slf4j
//@Data
//public class ConsistentHash {
//
//    @Resource
//    private ShardingDataSource shardingDataSource;
//
//    // 所有数据源对应其的虚拟节点
//    private HashMap<String, SortedMap<Long, String>> tableVirtualNodes = new HashMap<>();
//
//    private Map<String, List<String>> realTableNodes = new HashMap<>();
//
//    public ConsistentHash() {
//        init();
//
//    }
//
//    // 主要就做了初始化hash环状 就是使用了一个红黑树的treeMap，然后根据key的一个hashCode进行排序
//    public void init() {
//        try {
//            ShardingRule rule = shardingDataSource.getRuntimeContext().getRule();
//            // 获取所有的表规则
//            Collection<TableRule> tableRules = rule.getTableRules();
//            // 遍历所有逻辑表
//            for (TableRule tableRule : tableRules) {
//                String logicTable = tableRule.getLogicTable();
//                List<String> tableNames = tableRule.getActualDataNodes()
//                        .stream()
//                        .map(DataNode::getTableName)
//                        .collect(Collectors.toList());
//                tableVirtualNodes.put(logicTable, initNodesToHashLoop(tableNames));
//                realTableNodes.put(logicTable, tableNames);
//                log.info("logicTable is {}", logicTable);
//            }
//            log.info("tableRules is {}", tableRules);
//
//        } catch (Exception e) {
//            log.error("分表节点初始化失败 {}", e);
//        }
//    }
//
//    public static final Integer VIRTUAL_NODES = 3;
//
//    public SortedMap<Long, String> initNodesToHashLoop(Collection<String> tableNodes) {
//        SortedMap<Long, String> virtualTableNodes = new TreeMap<>();
//        for (String node : tableNodes) {
//            for (int i = 0; i < VIRTUAL_NODES; i++) {
//                String s = String.valueOf(i);
//                // 虚拟节点的hash值
//                String virtualNodeName = node + "-VN" + s;
//                long hash = getHash(virtualNodeName);
//                virtualTableNodes.put(hash, virtualNodeName);
//            }
//        }
//
//        return virtualTableNodes;
//    }
//
//    public long getHash(String key) {
//        final int p = 16777619;
//        int hash = (int) 2166136261L;
//        for (int i = 0; i < key.length(); i++)
//            hash = (hash ^ key.charAt(i)) * p;
//        hash += hash << 13;
//        hash ^= hash >> 7;
//        hash += hash << 3;
//        hash ^= hash >> 17;
//        hash += hash << 5;
//
//        // 如果算出来的值为负数则取其绝对值
//        if (hash < 0) hash = Math.abs(hash);
//
////        log.info("计算得到 hash为{}", hash);
//        return hash;
//    }
//
//    /**
//     * 通过计算key的hash
//     * 计算映射的表节点 根据key进行table路由
//     *
//     * @param key
//     * @return
//     */
//    public String getTableNode(String logicTable, String key) {
//        String virtualNode = getVirtualTableNode(logicTable, key);
//        //虚拟节点名称截取后获取真实节点
//        if (StringUtils.isNotBlank(virtualNode)) {
//            String tableName = virtualNode.substring(0, virtualNode.indexOf("-"));
//            return tableName;
//        }
//        return null;
//    }
//
//    /**
//     * 根据逻辑表和路由key获取对物理表节点
//     * @param key
//     * @return
//     */
//    public String getVirtualTableNode(String logicTable, String key) {
//        SortedMap<Long, String> virtualTables = tableVirtualNodes.get(logicTable);
//        // 获取到对应逻辑表的环
//        long hash = getHash(key);
//        // 得到大于该Hash值的所有Map
//        SortedMap<Long, String> subMap = virtualTables.tailMap(hash);
//        String virtualNode;
//        if (subMap.isEmpty()) {
//            //如果没有比该key的hash值大的，则从第一个node开始
//            Long i = virtualTables.firstKey();
//            //返回对应的服务器
//            virtualNode = virtualTables.get(i);
//        } else {
//            //第一个Key就是顺时针过去离node最近的那个结点
//            Long i = subMap.firstKey();
//            //返回对应的服务器
//            virtualNode = subMap.get(i);
//        }
//        return virtualNode;
//    }
//
//    public List<String> needToMigrateTables(List<String> needToHalveTables, Integer addNum, String logicTable) {
//        List<String> realTables = realTableNodes.get(logicTable);
//        if (addNum == null) addNum = needToHalveTables.size();
//        return needToMigrateTables(needToHalveTables, addNum, logicTable);
//    }
//
//    private String getRealTablePrefix(String realTableName) {
//        return realTableName.substring(0, 7);
//
//    }
//    /**
//     * *
//     * @param needToHalveTables 需要迁移的表 如t_user_1
//     * @param addNum 加表数量，如过不指定，则是根据needToHalveTables数量加
//     * @param tables 真实表数量
//     * @return
//     */
//    public List<String> needToMigrateTables(List<String> needToHalveTables, Integer addNum, List<String> tables) {
//        // 若没有指定添加的表数量则直接为需要迁移的表数量
//        String prefix = getRealTablePrefix(tables.get(0));
//        List<String> newTables = new ArrayList<>();
//
//        // 构建新的真实节点名称
//        for (int i = 0; i < addNum; i++) {
//            newTables.add(prefix + (tables.size() + i));
//        }
//        tables.addAll(newTables);
//
//        // 根据虚拟节点获取到真实表name t_user_1
//        String realName = getRealName(tables.get(0));
//        // 根据真实表名称获取到逻辑表名称
//        String logicName = getLogicName(realName);
//
//        // 获取到逻辑表新的所有的虚拟节点
//        SortedMap<Long, String> longStringSortedMap = initNodesToHashLoop(tables);
//
//        tableVirtualNodes.put(logicName, longStringSortedMap);
//        realTableNodes.put(logicName, newTables);
//
//        // 所有新表的虚拟节点
//        HashMap<String, Long> newVirtualNode = new HashMap<>();
//        for (String newTable : newTables) {
//            for (int i = 0; i < VIRTUAL_NODES; i++) {
//                String index = String.valueOf(i);
//                // 虚拟节点的hash值
//                String virtualNodeName = newTable + "-VN" + index;
//                long hash = getHash(virtualNodeName);
//                newVirtualNode.put(virtualNodeName, hash);
//            }
//        }
//
//        Set<String> set = get(newVirtualNode, longStringSortedMap, new HashSet<>(needToHalveTables));
//        System.out.println("一共需要扩容的有这些表" + set);
//        return null;
//    }
//
//    public Set<String> get(HashMap<String, Long> newVirtualNode, SortedMap<Long, String> longStringSortedMap, Set<String> set) {
//        List<String> res = new ArrayList<>();
//        System.out.println(longStringSortedMap.size());
//        for (Long value : newVirtualNode.values()) {
//            // 获取到比虚拟机节点要大
//            SortedMap<Long, String> large = longStringSortedMap.tailMap(value);
//            // 获取里面最小的，也就是比虚拟节点hash大得最少得节点
//            Long key = large.firstKey();
//            String realName = getRealName(large.get(key));
//            res.add(realName);
//        }
//
//        // 需要迁移得表 + 虚拟节点数据迁移的表
//        set.addAll(res);
//        return set;
//    }
//
//    public String getRealName(String vName) {
//        String[] vns = vName.split("-VN");
//        return vns[0];
//    }
//
//    public String getLogicName(String realName) {
//        // t_user_1 -> t_user
//        return realName.substring(0, 5);
//    }
//}
//
