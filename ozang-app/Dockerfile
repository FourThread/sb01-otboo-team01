FROM --platform=linux/amd64 amazoncorretto:17-alpine AS base
WORKDIR /app

# Gradle Wrapper와 설정 파일만 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle build.gradle ./

FROM base AS dependencies

COPY ozang-core/build.gradle ozang-core/
COPY ozang-app/build.gradle ozang-app/

RUN ./gradlew :ozang-core:dependencies --no-daemon --quiet || true
RUN ./gradlew :ozang-app:dependencies --no-daemon --quiet || true

FROM dependencies AS builder

COPY ozang-core/src/ ozang-core/src/
COPY ozang-app/src/ ozang-app/src/

RUN ./gradlew :ozang-app:build -x test --no-daemon --quiet

FROM --platform=linux/amd64 amazoncorretto:17-alpine
WORKDIR /app

# 환경 변수 설정
ENV PROJECT_NAME=OZANG-APP \
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
      description="O-ZANG Web Application optimized for t3.small" \
      platform="linux/amd64" \
      instance-type="t3.small" \
      cpu="2-vcpu" \
      memory="2gb" \
      application-type="web-application"

ENV JAVA_OPTS="-Xmx1024m \
    -Xms512m \
    -XX:MaxMetaspaceSize=256m \
    -XX:MetaspaceSize=128m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=25.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Seoul \
    -Dspring.output.ansi.enabled=never"

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

# 로그 디렉토리 생성 (최소화)
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /app/ozang-app/build/libs/ozang-app.jar app.jar

# 사용자 전환
USER appuser

# 포트 노출
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "\
    exec java ${JAVA_OPTS} -jar app.jar \
    --server.port=8080 \
    --spring.profiles.active=prod \
    --spring.application.name=${PROJECT_NAME} \
"]