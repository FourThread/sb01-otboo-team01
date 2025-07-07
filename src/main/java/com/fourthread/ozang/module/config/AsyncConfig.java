package com.fourthread.ozang.module.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableRetry
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 외부 API 호출용 ThreadPool
     * 기상청 API와 카카오 API 병렬 호출에 사용
     * I/O 집약적 작업 - 코어 스레드 수 높게 설정
     */
    @Bean(name = "apiCallExecutor")
    public Executor apiCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 코어 스레드 수: CPU 코어 수 * 2
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(corePoolSize);

        //최대 스레드 수: CPU 코어 수 * 4
        int maxPoolSize = Runtime.getRuntime().availableProcessors() * 4;
        executor.setMaxPoolSize(maxPoolSize);

        // 최대 100개 작업까지 대기
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("ApiCall-");

        // 스레드 유지 시간
        executor.setKeepAliveSeconds(60);

        //거부 정책: 호출자 스레드에서 실행
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());

        // 애플리케이션 종료 시 스레드 풀 정리
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("API 호출용 ThreadPoll 설정 완료 - Core={}, Max={}, Queue={}",
            corePoolSize, maxPoolSize, executor.getQueueCapacity());

        return executor;
    }
}
