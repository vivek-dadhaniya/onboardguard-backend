package com.onboardguard.shared.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async and scheduling infrastructure for OnboardGuard.
 *
 * @EnableAsync  — activates @Async on all Spring beans.
 * @EnableScheduling — activates @Scheduled on SlaMonitoringJob.
 *
 * Who uses the async executor in this project:
 *
 *  EmailService          → @Async on every send*() method:
 *                           sendWelcomeEmail, sendAcknowledgementEmail,
 *                           sendScreeningClearedEmail, sendAlertNotificationEmail,
 *                           sendDocRejectionEmail, sendCaseResolvedEmail,
 *                           sendTempPasswordEmail
 *
 *  UserManagementService → createUser() fires sendTempPasswordEmail() async
 *                          so the HTTP response returns immediately
 *
 *  DocumentVerification  → rejectDocument() fires sendDocRejectionEmail() async
 *
 *  SlaMonitoringJob      → @Scheduled(fixedRate = 1_800_000) — runs every 30 min
 *                          in the scheduling thread (separate from this pool)
 *
 * Thread pool is tuned for email-heavy I/O workload:
 *  - Core threads kept alive for bursty email sends
 *  - Queue absorbs spikes (e.g. bulk alert notifications to all L1/L2 officers)
 *  - CallerRunsPolicy — if pool + queue are full, the calling thread runs the
 *    task itself instead of dropping it (no email ever gets silently lost)
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${async.core-pool-size}")
    private int corePoolSize;

    @Value("${async.max-pool-size}")
    private int maxPoolSize;

    @Value("${async.queue-capacity}")
    private int queueCapacity;

    @Value("${async.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // Never silently drop a task — if pool is exhausted, the caller runs it
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for active email tasks to finish before shutting down the app
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Async executor initialised — core: {}, max: {}, queue: {}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }
}