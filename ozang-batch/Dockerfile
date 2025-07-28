# 공통 베이스 스테이지 (app과 동일한 구조로 캐시 공유 가능)
FROM --platform=linux/amd64 amazoncorretto:17-alpine AS base
WORKDIR /app

# Gradle Wrapper와 설정 파일만 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle build.gradle ./

# 의존성 다운로드 전용 스테이지 (캐시 활용 극대화)
FROM base AS dependencies
# 빌드 스크립트만 복사해서 의존성 다운로드
COPY ozang-core/build.gradle ozang-core/
COPY ozang-batch/build.gradle ozang-batch/

# 의존성만 다운로드 (소스코드 변경 시에도 캐시 유지)
RUN ./gradlew :ozang-core:dependencies --no-daemon --quiet || true
RUN ./gradlew :ozang-batch:dependencies --no-daemon --quiet || true

FROM dependencies AS builder

# 소스코드 복사 (의존성 캐시는 유지됨)
COPY ozang-core/src/ ozang-core/src/
COPY ozang-batch/src/ ozang-batch/src/

# 배치 애플리케이션 빌드 (테스트 제외로 빌드 시간 단축)
RUN ./gradlew :ozang-batch:build -x test --no-daemon --quiet

FROM --platform=linux/amd64 amazoncorretto:17-alpine
WORKDIR /app

# 환경 변수 설정 (배치 전용)
ENV PROJECT_NAME=OZANG-BATCH \
    TZ=Asia/Seoul
ARG VERSION
ARG BUILD_DATE
ARG VCS_REF
ENV PROJECT_VERSION=${VERSION}

# 빌드 정보를 레이블로 추가
LABEL maintainer="fourthread" \
      version="${VERSION}" \
      build-date="${BUILD_DATE}" \
      vcs-ref="${VCS_REF}" \
      description="O-ZANG Batch Application optimized for t3.small" \
      instance-type="t3.small" \
      cpu="2-vcpu" \
      memory="2gb" \
      platform="linux/amd64" \
      application-type="batch-processor"

# 배치 프로세서 최적화된 JAVA_OPTS
ENV JAVA_OPTS="-Xmx1024m \
    -Xms512m \
    -XX:MaxMetaspaceSize=256m \
    -XX:MetaspaceSize=128m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/batch-heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=25.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Seoul \
    -Dspring.output.ansi.enabled=never \
    -Dspring.profiles.active=batch"

RUN apk update && apk upgrade && \
    apk add --no-cache \
        tzdata \
        curl && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

# 애플리케이션 사용자 생성
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup -s /bin/sh -D

# 배치 처리용 디렉토리 생성 (최소화)
RUN mkdir -p /app/logs /app/batch-temp && \
    chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /app/ozang-batch/build/libs/ozang-batch.jar app.jar

# 사용자 전환
USER appuser

# 포트 노출 (관리 API용)
EXPOSE 8081

HEALTHCHECK --interval=60s --timeout=15s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "\
    exec java ${JAVA_OPTS} -jar app.jar \
    --server.port=8081 \
    --spring.profiles.active=batch,prod \
    --spring.application.name=${PROJECT_NAME} \
    --spring.batch.job.enabled=true \
"]