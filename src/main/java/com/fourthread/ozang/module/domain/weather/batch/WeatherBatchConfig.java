package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class WeatherBatchConfig {
//
//    private final WeatherService weatherService;
//
//    @Bean
//    public Job weatherDataCleanupJob(JobRepository jobRepository, Step weatherDataCleanupStep) {
//        return new JobBuilder("weatherDataCleanupJob", jobRepository)
//            .start(weatherDataCleanupStep)
//            .build();
//    }
//
//    @Bean
//    public Step weatherDataCleanupStep(JobRepository jobRepository,
//        PlatformTransactionManager transactionManager,
//        Tasklet weatherDataCleanupTasklet) {
//        return new StepBuilder("weatherDataCleanupStep", jobRepository)
//            .tasklet(weatherDataCleanupTasklet, transactionManager)
//            .build();
//    }
//
//    @Bean
//    public Tasklet weatherDataCleanupTasklet() {
//        return (contribution, chunkContext) -> {
//            log.info("Starting weather data cleanup batch job");
//            weatherService.cleanupOldData();
//            log.info("Weather data cleanup batch job completed");
//            return RepeatStatus.FINISHED;
//        };
//    }
//}