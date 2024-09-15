package me.xgwd.idgenerate.template.core.snowflake;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.hutool.core.lang.Snowflake;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/14/22:30
 * @Description:
 */
@Slf4j
@Component
public class IdGeneratorSnowflake {

    private long workerId = 0;
    private long datacenterId = 1;
    private Snowflake snowflake = IdUtil.getSnowflake(workerId, datacenterId);



    @PostConstruct
    public void init() {
        try {
            workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr());
            log.info("当前机器的workerId:{}", workerId);
        } catch(Exception e) {
            e.printStackTrace();
            log.warn("当前机器的workerId获取失败", e);
            workerId = NetUtil.getLocalhostStr().hashCode();
        }
    }

    public synchronized long snowflakeId() {
        return snowflake.nextId();
    }

    public synchronized long snowflakeId(long workerId, long datacenterId) {
        Snowflake snowflake = IdUtil.getSnowflake(datacenterId, workerId);
        return snowflake.nextId();
    }

    // 测试
    public static void main(String[] args) throws InterruptedException {
        testGenerateIdPerSecondWithMultipleThreads();
    }


    public static void testGenerateIdPerSecondWithMultipleThreads() throws InterruptedException {
        // 以上过程只需全局一次，且应在生成ID之前完成。
        final int threadCount = 13; // 使用10个线程进行测试
        final int testDurationSeconds = 1; // 测试持续时间为1秒
        LongAdder longAdder = new LongAdder();
//        final AtomicLong count = new AtomicLong(0); // 使用原子变量来保证线程安全
        Set<Long> set = new ConcurrentHashSet<>();

        short s = 1;
        IdGeneratorSnowflake idGeneratorSnowflake = new IdGeneratorSnowflake();

        // 创建一个固定大小的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(13);

        long startTime = System.currentTimeMillis();
        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                while (System.currentTimeMillis() - startTime < testDurationSeconds * 1000) {
                    long l = idGeneratorSnowflake.snowflakeId();
//                    if (set.contains(l)) {
//                        System.out.println(l);
//                    }

//                    set.add(l);
                    longAdder.add(1);
                }
            });
        }
        // 关闭线程池并等待所有任务完成
        executorService.shutdown();

        executorService.awaitTermination(testDurationSeconds + 1, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        // 输出结果
        System.out.println("在 " + testDurationSeconds + " 秒内生成了 " + longAdder.sum() + " 个ID。");
        System.out.println("平均每秒生成 " + (longAdder.sum() / testDurationSeconds) + " 个ID。");
        System.out.println(set.size());
        // 可以添加断言来确保生成的ID数量在预期范围内
    }
}

