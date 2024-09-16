package me.xgwd.idgenerate.core.test;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/0:27
 * @Description:
 */
public class TestIDGenerator {
    public static void main(String[] args) throws InterruptedException {
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        TestIDGenerator testIDGenerator = new TestIDGenerator();
        testIDGenerator.test(snowflake::nextId, false);
    }


    public void test(Supplier<Long> supplier, boolean enableCheckDuplicate) throws InterruptedException {
        // 以上过程只需全局一次，且应在生成ID之前完成。
        final int threadCount = Runtime.getRuntime().availableProcessors() + 1;  // 使用10个线程进行测试
        final int testDurationSeconds = 1; // 测试持续时间为1秒
        LongAdder longAdder = new LongAdder();
//        final AtomicLong count = new AtomicLong(0); // 使用原子变量来保证线程安全
        Set<Long> set = new ConcurrentHashSet<>();


        // 创建一个固定大小的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        long startTime = System.currentTimeMillis();
        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                while (System.currentTimeMillis() - startTime < testDurationSeconds * 1000) {
                    Long l = supplier.get();
                    if (enableCheckDuplicate) {
                         if (set.contains(l)) {
                             System.out.println(l);
                         }
                        set.add(l);
                    }
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
