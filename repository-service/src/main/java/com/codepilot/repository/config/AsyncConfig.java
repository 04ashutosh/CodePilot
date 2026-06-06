package com.codepilot.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables async processing for repository sync operations.
 * Without this, @Async annotations on service methods would be ignored.
 *
 * We use a bounded thread pool to prevent overwhelming the system
 * if multiple repos are synced simultaneously.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "repoSyncExecutor")
    public Executor repoSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);       // 2 concurrent syncs by default
        executor.setMaxPoolSize(4);        // Scale up to 4 under load
        executor.setQueueCapacity(10);     // Queue up to 10 pending syncs
        executor.setThreadNamePrefix("repo-sync-");
        executor.initialize();
        return executor;
    }
}