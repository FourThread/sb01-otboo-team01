package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.repository.NotificationRepository;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * =============== 새로운 배치 작업 추가 ===============
 * 날씨 알림 발송 배치
 * - 특정 날씨 조건(폭우, 폭설, 한파, 폭염 등)에 대한 사용자 알림 발송
 * - 사용자의 위치 기반으로 맞춤형 알림 제공
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeatherAlertBatch {

    private final WeatherRepository weatherRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CoordinateConverter coordinateConverter;
    private final EntityManagerFactory entityManagerFactory;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Value("${batch.weather.alert.chunk-size:50}")
    private int chunkSize;

    /**
     * =============== 날씨 알림 Job ===============
     */
    @Bean
    public Job weatherAlertJob(
        JobRepository jobRepository,
        Step detectWeatherAlertsStep,
        Step sendUserNotificationsStep,
        Step generateAlertReportStep
    ) {
        return new JobBuilder("weatherAlertJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(detectWeatherAlertsStep)
            .next(sendUserNotificationsStep)
            .next(generateAlertReportStep)
            .build();
    }

    /**
     * Step 1: 날씨 경보 감지
     */
    @Bean
    public Step detectWeatherAlertsStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("detectWeatherAlertsStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("날씨 경보 감지 시작");

                // 오늘과 내일의 날씨 데이터 조회
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime tomorrow = now.plusDays(1);

                List<Weather> upcomingWeathers = weatherRepository.findWeatherDataBetweenDates(
                    now, tomorrow.withHour(23).withMinute(59)
                );

                // 지역별로 그룹화
                Map<String, List<Weather>> weatherByLocation = upcomingWeathers.stream()
                    .collect(Collectors.groupingBy(w ->
                        w.getLocation().locationNames().get(0)
                    ));

                // 경보 대상 날씨 감지
                List<WeatherAlert> alerts = new ArrayList<>();

                weatherByLocation.forEach((location, weathers) -> {
                    for (Weather weather : weathers) {
                        WeatherAlert alert = checkWeatherAlert(weather, location);
                        if (alert != null) {
                            alerts.add(alert);
                        }
                    }
                });

                // 경보 데이터를 컨텍스트에 저장
                ExecutionContext executionContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

                executionContext.put("weatherAlerts", alerts);
                executionContext.putInt("alertCount", alerts.size());

                log.info("=============== 날씨 경보 감지 완료 ===============");
                log.info("감지된 경보: {}건", alerts.size());

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     *  Step 2: 사용자 알림 발송
     */
    @Bean
    public Step sendUserNotificationsStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("sendUserNotificationsStep", jobRepository)
            .<WeatherAlert, List<Notification>>chunk(chunkSize, transactionManager)
            .reader(weatherAlertReader())
            .processor(notificationProcessor())
            .writer(notificationWriter())
            .faultTolerant()
            .skipLimit(10)
            .skip(Exception.class)
            .build();
    }

    /**
     *  Step 3: 알림 발송 리포트 생성
     */
    @Bean
    public Step generateAlertReportStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("generateAlertReportStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("알림 발송 리포트 생성");

                ExecutionContext jobContext = chunkContext.getStepContext()
                    .getJobExecution()
                    .getExecutionContext();

                int totalAlerts = jobContext.getInt("alertCount", 0);
                int totalNotifications = jobContext.getInt("totalNotificationsSent", 0);

                @SuppressWarnings("unchecked")
                Map<String, Integer> alertTypeCount =
                    (Map<String, Integer>) jobContext.get("alertTypeCount");

                log.info("=============== 날씨 알림 발송 리포트 ===============");
                log.info("감지된 경보 수: {}건", totalAlerts);
                log.info("발송된 알림 수: {}건", totalNotifications);

                if (alertTypeCount != null) {
                    log.info("경보 유형별 통계:");
                    alertTypeCount.forEach((type, count) ->
                        log.info("  - {}: {}건", type, count));
                }

                // 발송률 계산
                double sendRate = totalAlerts > 0
                    ? (double) totalNotifications / totalAlerts * 100
                    : 0;
                log.info("알림 발송률: {:.2f}%", sendRate);

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * ItemReader: 날씨 경보 읽기
     */
    @Bean
    @StepScope
    public ItemReader<WeatherAlert> weatherAlertReader(
        @Value("#{jobExecutionContext['weatherAlerts']}") List<WeatherAlert> weatherAlerts
    ) {
        return new ItemReader<WeatherAlert>() {
            private int index = 0;

            @Override
            public WeatherAlert read() throws Exception {
                if (weatherAlerts != null && index < weatherAlerts.size()) {
                    return weatherAlerts.get(index++);
                }
                return null;
            }
        };
    }

    /**
     *  ItemProcessor: 알림 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<WeatherAlert, List<Notification>> notificationProcessor() {
        return weatherAlert -> {
            log.debug("날씨 경보 {} 에 대한 알림 생성", weatherAlert.getAlertType());

            List<Notification> notifications = new ArrayList<>();

            // 해당 지역의 사용자 조회
            List<User> affectedUsers = findUsersInLocation(weatherAlert.getLocation());

            for (User user : affectedUsers) {
                // 사용자별 맞춤형 알림 생성
                Notification notification = createNotification(user, weatherAlert);
                if (notification != null) {
                    notifications.add(notification);
                }
            }

            log.info("경보 {} - 대상 사용자: {}명, 생성된 알림: {}개",
                weatherAlert.getAlertType(), affectedUsers.size(), notifications.size());

            return notifications;
        };
    }

    /**
     *  ItemWriter: 알림 저장
     */
    @Bean
    @StepScope
    public ItemWriter<List<Notification>> notificationWriter() {
        return chunk -> {
            int totalNotifications = 0;
            Map<String, Integer> alertTypeCount = new HashMap<>();

            for (List<Notification> notifications : chunk) {
                // 알림 저장
                notificationRepository.saveAll(notifications);
                totalNotifications += notifications.size();

                // 통계 수집
                for (Notification notification : notifications) {
                    String alertType = extractAlertType(notification.getTitle());
                    alertTypeCount.merge(alertType, 1, Integer::sum);
                }
            }

            // Job 컨텍스트에 통계 저장
            StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
            ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();

            int existingTotal = jobContext.getInt("totalNotificationsSent", 0);
            jobContext.putInt("totalNotificationsSent", existingTotal + totalNotifications);
            jobContext.put("alertTypeCount", alertTypeCount);

            log.info("알림 {} 건 저장 완료", totalNotifications);
        };
    }

    /**
     * =============== 날씨 경보 체크 로직 ===============
     */
    private WeatherAlert checkWeatherAlert(Weather weather, String location) {
        WeatherAlert alert = null;

        // 폭염 경보 (33도 이상)
        if (weather.getTemperature().max() >= 33) {
            alert = new WeatherAlert();
            alert.setAlertType("HEAT_WAVE");
            alert.setLevel(NotificationLevel.WARNING);
            alert.setTitle("폭염주의보 발령");
            alert.setMessage(String.format("내일 %s 지역 최고기온이 %d°C까지 오를 예정입니다. 야외활동을 자제하고 수분을 충분히 섭취하세요.",
                location, Math.round(weather.getTemperature().max())));
            alert.setWeather(weather);
            alert.setLocation(location);
            alert.setAlertTime(weather.getForecastAt());
        }

        // 한파 경보 (-12도 이하)
        else if (weather.getTemperature().min() <= -12) {
            alert = new WeatherAlert();
            alert.setAlertType("COLD_WAVE");
            alert.setLevel(NotificationLevel.WARNING);
            alert.setTitle("한파주의보 발령");
            alert.setMessage(String.format("내일 %s 지역 최저기온이 %d°C까지 내려갈 예정입니다. 따뜻한 옷을 준비하시고 동파에 주의하세요.",
                location, Math.round(weather.getTemperature().min())));
            alert.setWeather(weather);
            alert.setLocation(location);
            alert.setAlertTime(weather.getForecastAt());
        }

        // 호우 경보 (시간당 30mm 이상)
        else if (weather.getPrecipitation().amount() >= 30) {
            alert = new WeatherAlert();
            alert.setAlertType("HEAVY_RAIN");
            alert.setLevel(NotificationLevel.WARNING);
            alert.setTitle("호우주의보 발령");
            alert.setMessage(String.format("내일 %s 지역에 시간당 %dmm의 많은 비가 예상됩니다. 우산을 꼭 챙기시고 침수에 주의하세요.",
                location, Math.round(weather.getPrecipitation().amount())));
            alert.setWeather(weather);
            alert.setLocation(location);
            alert.setAlertTime(weather.getForecastAt());
        }

        // 대설 경보 (5cm 이상)
        else if (weather.getPrecipitation().type() == PrecipitationType.SNOW
            && weather.getPrecipitation().amount() >= 5) {
            alert = new WeatherAlert();
            alert.setAlertType("HEAVY_SNOW");
            alert.setLevel(NotificationLevel.WARNING);
            alert.setTitle("대설주의보 발령");
            alert.setMessage(String.format("내일 %s 지역에 %dcm의 많은 눈이 예상됩니다. 미끄러운 길에 주의하시고 대중교통을 이용하세요.",
                location, Math.round(weather.getPrecipitation().amount())));
            alert.setWeather(weather);
            alert.setLocation(location);
            alert.setAlertTime(weather.getForecastAt());
        }

        // 강풍 경보 (14m/s 이상)
        else if (weather.getWindSpeed().asWord() == WindStrength.STRONG) {
            alert = new WeatherAlert();
            alert.setAlertType("STRONG_WIND");
            alert.setLevel(NotificationLevel.INFO);
            alert.setTitle("강풍주의보 발령");
            alert.setMessage(String.format("내일 %s 지역에 초속 %dm의 강한 바람이 예상됩니다. 간판 등 낙하물에 주의하세요.",
                location, Math.round(weather.getWindSpeed().speed())));
            alert.setWeather(weather);
            alert.setLocation(location);
            alert.setAlertTime(weather.getForecastAt());
        }

        return alert;
    }

    /**
     * 사용자 조회 로직
     */
    private List<User> findUsersInLocation(String location) {
        // 실제 구현에서는 사용자의 위치 정보를 기반으로 조회
        // 여기서는 예시로 해당 지역명을 포함하는 사용자 조회
        return userRepository.findByProfileLocationContaining(location);
    }

    /**
     *  알림 생성 로직
     */
    private Notification createNotification(User user, WeatherAlert alert) {
        // 사용자의 알림 설정 확인
        if (!user.isWeatherAlertEnabled()) {
            return null;
        }

        Notification notification = new Notification();
        notification.setReceiver(user);
        notification.setTitle(alert.getTitle());
        notification.setContent(personalizeAlertMessage(alert.getMessage(), user));
        notification.setLevel(alert.getLevel());
        notification.setCreatedAt(LocalDateTime.now());

        // 메타데이터 추가
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertType", alert.getAlertType());
        metadata.put("weatherId", alert.getWeather().getId());
        metadata.put("alertTime", alert.getAlertTime());
        notification.setMetadata(metadata);

        return notification;
    }

    /**
     * 메시지 개인화
     */
    private String personalizeAlertMessage(String message, User user) {
        // 사용자의 온도 민감도에 따른 추가 조언
        if (user.getProfile() != null && user.getProfile().getTemperatureSensitivity() != null) {
            int sensitivity = user.getProfile().getTemperatureSensitivity();

            if (sensitivity >= 4 && message.contains("폭염")) {
                message += " 더위에 민감하신 편이니 특히 주의하세요.";
            } else if (sensitivity <= 2 && message.contains("한파")) {
                message += " 추위에 민감하신 편이니 특히 주의하세요.";
            }
        }

        return message;
    }

    private String extractAlertType(String title) {
        if (title.contains("폭염")) return "HEAT_WAVE";
        if (title.contains("한파")) return "COLD_WAVE";
        if (title.contains("호우")) return "HEAVY_RAIN";
        if (title.contains("대설")) return "HEAVY_SNOW";
        if (title.contains("강풍")) return "STRONG_WIND";
        return "OTHER";
    }

    /**
     * DTO 클래스 정의
     */
    private static class WeatherAlert {
        private String alertType;
        private NotificationLevel level;
        private String title;
        private String message;
        private Weather weather;
        private String location;
        private LocalDateTime alertTime;

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }

        public NotificationLevel getLevel() { return level; }
        public void setLevel(NotificationLevel level) { this.level = level; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Weather getWeather() { return weather; }
        public void setWeather(Weather weather) { this.weather = weather; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public LocalDateTime getAlertTime() { return alertTime; }
        public void setAlertTime(LocalDateTime alertTime) { this.alertTime = alertTime; }
    }
}