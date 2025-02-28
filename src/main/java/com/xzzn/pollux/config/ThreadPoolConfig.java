package com.xzzn.pollux.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 这里设计结构化数据导入,或标注数据导入的线程池
 */
@Configuration
public class ThreadPoolConfig
{
    @Bean(name = "fileUploadTaskExecutor")
    public AsyncTaskExecutor fileUploadTaskExecutor()
    {
        return createDefaultThreadPoolExecutor("FileUpload--", 10, 5, 10_000_000, 60);
    }

    @Bean(name = "generateQATaskExecutor")
    public AsyncTaskExecutor generateQATaskExecutor()
    {
        return createDefaultThreadPoolExecutor("QATask--", 10, 5, 10_000_000, 60);
    }

    @Bean(name = "trainProgressExecutor")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(5); // 根据需要调整线程池大小
    }

    private ThreadPoolTaskExecutor createDefaultThreadPoolExecutor(String namePrefix, Integer maxPoolSize, Integer corePoolSize, Integer queueCapacity, Integer keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(namePrefix);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAllowCoreThreadTimeOut(true);
        return executor;
    }

    private ThreadPoolTaskExecutor createFixedThreadPoolExecutor(int size, String namePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //最大线程数
        executor.setMaxPoolSize(size);
        //核心线程数
        executor.setCorePoolSize(size);
        //任务队列的大小
        executor.setQueueCapacity(0);
        //线程前缀名
        executor.setThreadNamePrefix(namePrefix);
        //线程存活时间
        executor.setKeepAliveSeconds(0);

        /**
         * 拒绝处理策略
         * CallerRunsPolicy()：交由调用方线程运行,比如 main 线程。
         * AbortPolicy()：直接抛出异常。
         * DiscardPolicy()：直接丢弃。
         * DiscardOldestPolicy()：丢弃队列中最老的任务。
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        //线程初始化
        executor.initialize();
        return executor;
    }
}