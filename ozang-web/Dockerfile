FROM --platform=linux/amd64 amazoncorretto:17-alpine AS builder
WORKDIR /app

# Gradle Wrapper와 설정 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY settings.gradle build.gradle ./

# 모듈 구조 복사
COPY ozang-core/ ozang-core/
COPY ozang-web/ ozang-web/

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew :ozang-core:dependencies --no-daemon --quiet
RUN ./gradlew :ozang-web:dependencies --no-daemon --quiet

# 웹 모듈 빌드
RUN ./gradlew :ozang-web:build -x test --no-daemon --quiet

FROM --platform=linux/amd64 amazoncorretto:17-alpine
WORKDIR /app

# 환경 변수 설정 (웹 전용)
ENV PROJECT_NAME=OZANG-WEB
ARG VERSION
ARG BUILD_DATE
ARG VCS_REF
ENV PROJECT_VERSION=${VERSION}
ENV TZ=Asia/Seoul

# 빌드 정보를 레이블로 추가
LABEL maintainer="fourthread" \
      version="${VERSION}" \
      build-date="${BUILD_DATE}" \
      vcs-ref="${VCS_REF}" \
      description="O-ZANG Web Application optimized for t3.small" \
      instance-type="t3.small" \
      cpu="2-vcpu" \
      memory="2gb" \
      platform="linux/amd64" \
      application-type="web-server"

# 웹 서버 최적화된 JAVA_OPTS
ENV JAVA_OPTS="-Xmx1024m \
    -Xms512m \
    -XX:MaxMetaspaceSize=256m \
    -XX:MetaspaceSize=128m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/web-heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=25.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:MinRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Seoul \
    -Dspring.output.ansi.enabled=never \
    -Dspring.profiles.active=web,prod"

# 시간대 설정 및 필수 패키지 설치
RUN apk update && apk upgrade && \
    apk add --no-cache \
        tzdata \
        curl \
        wget \
        jq \
        procps \
        htop && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

# 애플리케이션 사용자 생성
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup -s /bin/sh -D

# 로그 및 데이터 디렉토리 생성
RUN mkdir -p /app/logs /app/data /app/uploads && \
    chown -R appuser:appgroup /app

# 웹 애플리케이션 JAR 복사
COPY --from=builder --chown=appuser:appgroup /app/ozang-web/build/libs/*.jar app.jar

# 애플리케이션 정보 파일 생성
RUN echo "Application: ${PROJECT_NAME}" > /app/web-app-info.txt && \
    echo "Version: ${PROJECT_VERSION:-unknown}" >> /app/web-app-info.txt && \
    echo "Build Date: ${BUILD_DATE:-unknown}" >> /app/web-app-info.txt && \
    echo "Git Commit: ${VCS_REF:-unknown}" >> /app/web-app-info.txt && \
    echo "Java Version: $(java -version 2>&1 | head -n 1)" >> /app/web-app-info.txt && \
    echo "Optimized for: t3.small (2 vCPU, 2GB RAM)" >> /app/web-app-info.txt && \
    echo "Application Type: Web Server" >> /app/web-app-info.txt && \
    echo "Build Time: $(date)" >> /app/web-app-info.txt && \
    chown appuser:appgroup /app/web-app-info.txt

# 빌드 검증
RUN if [ ! -f app.jar ]; then \
        echo "ERROR: app.jar not found!" && exit 1; \
    fi

# 포트 노출
EXPOSE 8080

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]