package me.xgwd.idgenerate.template;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Snowflake;
import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/14/9:35
 * @Description:
 */
@Slf4j
public abstract class AbstractWorkIdChooseTemplate {

    public static void testGenerateIdPerSecondWithMultipleThreads() throws InterruptedException {
//        // 创建 IdGeneratorOptions 对象，可在构造函数中输入 WorkerId：
//        IdGeneratorOptions options = new IdGeneratorOptions(Your_Unique_Worker_Id);
//        // options.WorkerIdBitLength = 10; // 默认值6，限定 WorkerId 最大值为2^6-1，即默认最多支持64个节点。
//        // options.SeqBitLength = 6; // 默认值6，限制每毫秒生成的ID个数。若生成速度超过5万个/秒，建议加大 SeqBitLength 到 10。
//        // options.BaseTime = Your_Base_Time; // 如果要兼容老系统的雪花算法，此处应设置为老系统的BaseTime。
//        // ...... 其它参数参考 IdGeneratorOptions 定义。
//
//        // 保存参数（务必调用，否则参数设置不生效）：
//        YitIdHelper.SetIdGenerator(options);

        // 以上过程只需全局一次，且应在生成ID之前完成。
        final int threadCount = Runtime.getRuntime().availableProcessors() + 1; // 使用10个线程进行测试
        final int testDurationSeconds = 1; // 测试持续时间为1秒
        LongAdder longAdder = new LongAdder();
//        final AtomicLong count = new AtomicLong(0); // 使用原子变量来保证线程安全
        Set<Long> set = new ConcurrentHashSet<>();

        short s = 1;
        IdGeneratorOptions options = new IdGeneratorOptions(s);
        YitIdHelper.setIdGenerator(options);
        // 创建一个固定大小的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        long startTime = System.currentTimeMillis();
        // 启动所有线程
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                while (System.currentTimeMillis() - startTime < testDurationSeconds * 1000) {
                    long l = YitIdHelper.nextId();
                    if (set.contains(l)) {
                        System.out.println(l);
                    }

                    set.add(l);
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
    public static void main(String[] args) throws InterruptedException {
        testGenerateIdPerSecondWithMultipleThreads();

    }
//    @Value("${framework.distributed.id.snowflake.is-use-system-clock:false}")
//    private boolean isUseSystemClock;

    /**
     * 根据自定义策略获取 WorkId 生成器
     *
     * @return
     */
    protected abstract WorkIdWrapper chooseWorkId();

    /**
     * 选择 WorkId 并初始化雪花
     */
//    public void chooseAndInit() {
//        // 模板方法模式: 通过抽象方法获取 WorkId 包装器创建雪花算法
//        WorkIdWrapper workIdWrapper = chooseWorkId();
//        long workId = workIdWrapper.getWorkId();
//        long dataCenterId = workIdWrapper.getDataCenterId();
//        Snowflake snowflake = new Snowflake(workId, dataCenterId, isUseSystemClock);
//        log.info("Snowflake type: {}, workId: {}, dataCenterId: {}", this.getClass().getSimpleName(), workId, dataCenterId);
//        SnowflakeIdUtil.initSnowflake(snowflake);
//    }
}
