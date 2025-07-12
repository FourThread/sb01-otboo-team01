FROM amazoncorretto:17-alpine AS builder
WORKDIR /app

# Gradle Wrapper와 설정 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon --quiet

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew clean build -x test --no-daemon --quiet

FROM amazoncorretto:17-alpine
WORKDIR /app

# 환경 변수 설정
ENV PROJECT_NAME=O-ZANG
ARG VERSION=v1.0.0
ENV PROJECT_VERSION=${VERSION}
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Xmx512m \
-Xms256m \
-XX:MaxMetaspaceSize=128m \
-XX:MetaspaceSize=64m \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:G1HeapRegionSize=16m \
-XX:+UseCompressedOops \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/app/logs/heapdump.hprof \
-XX:+ExitOnOutOfMemoryError \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseCGroupMemoryLimitForHeap \
-Djava.security.egd=file:/dev/./urandom \
-Dfile.encoding=UTF-8 \
-Duser.timezone=Asia/Seoul"

# 시간대 설정 및 필수 패키지 설치
RUN apk add --no-cache \
    tzdata \
    curl \
    && cp /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone \
    && apk del tzdata

# 애플리케이션 사용자 생성 (보안)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 로그 디렉토리 생성
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 빌드된 JAR 파일 복사
COPY --from=builder --chown=appuser:appgroup /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

# 사용자 전환
USER appuser

# 포트 노출
EXPOSE 80

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:80/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar --server.port=80 --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]