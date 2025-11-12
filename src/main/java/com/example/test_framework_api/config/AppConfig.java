package com.example.test_framework_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for parallel test execution.
 * Provides separate thread pools for UI and API tests to prevent resource contention.
 */
@Configuration
@EnableAsync
public class AppConfig {

    /**
     * Thread pool for UI test execution.
     * Limited to 4 threads to avoid browser instance overload.
     * Queue capacity allows buffering of pending tests.
     */
    @Bean(name = "uiTestExecutor")
    public Executor uiTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // Minimum threads
        executor.setMaxPoolSize(4);            // Maximum threads (UI limit)
        executor.setQueueCapacity(50);         // Pending test queue
        executor.setThreadNamePrefix("ui-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for API test execution.
     * Higher concurrency allowed since API tests are lightweight.
     */
    @Bean(name = "apiTestExecutor")
    public Executor apiTestExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);           // Minimum threads
        executor.setMaxPoolSize(8);            // Maximum threads (API limit)
        executor.setQueueCapacity(100);        // Larger queue for API tests
        executor.setThreadNamePrefix("api-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * General-purpose executor for non-test async tasks.
     */
    @Bean(name = "generalExecutor")
    public Executor generalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("general-");
        executor.initialize();
        return executor;
    }
}