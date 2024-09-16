package me.xgwd.common.excutor;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.common.build.ThreadFactoryBuilder;
import me.xgwd.common.build.ThreadPoolBuilder;
import me.xgwd.common.pool.TaskQueue;
import me.xgwd.common.proxy.RejectedProxyInvocationHandler;
import me.xgwd.common.proxy.RejectedProxyUtil;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/15:53
 * @Description:
 */
@Slf4j
public class EagerThreadPoolExecutor extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    public EagerThreadPoolExecutor(int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit unit,
                                   TaskQueue<Runnable> workQueue,
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }


    private final AtomicLong tasksNum = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicLong submitTask = new AtomicLong();
    public Long getSubmitTaskNum() {
        return submitTask.get();
    }

    @Override
    public void execute(Runnable task) {
        try {
            super.submit(task);
        } catch (RejectedExecutionException rejectedExecutionException) {
            System.out.println("aaa");
            // 进行一次重试
            TaskQueue<Runnable> queue = (TaskQueue<Runnable>) super.getQueue();

            try {
                // 没有走最大线程数 快速消费 直接就core和队列
                if (!queue.retryOffer(task, 0, TimeUnit.MILLISECONDS)) {
                    submitTask.decrementAndGet();
                    // 如果这里抛出异常那么工作线程就没了
                    throw new RejectedExecutionException("thread pool queue is full");
                }
            } catch (InterruptedException e) {
                submitTask.decrementAndGet();
                throw new RejectedExecutionException(e);
            }

        } catch (Exception e) {
            // 其他异常 处理不了
            submitTask.decrementAndGet();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        log.info("===================   线程池关闭~   ===================");
    }

    @Override
    public List<Runnable> shutdownNow() {
        log.info("===================   线程池马上关闭~   ===================");
        return super.shutdownNow();
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        startTime.set(System.currentTimeMillis());
        log.info("current thread {}'s task is starting",Thread.currentThread().getName());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        Long taskTime = System.currentTimeMillis() - startTime.get();
        tasksNum.incrementAndGet();
        totalTime.addAndGet(taskTime);
        log.info("current thread {}'s task is completed, task used {} ms", Thread.currentThread().getName(),
                System.currentTimeMillis() - startTime.get());

        log.info("activeCount:{}, queueSize:{}", this.getActiveCount(), this.getQueue().size());
    }

    @Override
    protected void terminated() {
        super.terminated();
        log.info("total task num is {}, avgTaskRuntime is {}", tasksNum.get(), totalTime.get() / tasksNum.get());
    }


}


