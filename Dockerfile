FROM amazoncorretto:17-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src/ src/
RUN ./gradlew clean build -x test --no-daemon

FROM amazoncorretto:17-alpine
WORKDIR /app

ENV PROJECT_NAME=O-ZANG
ARG VERSION=v1.0.0
ENV PROJECT_VERSION=${VERSION}
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Xmx768m -Xms512m -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=96m -XX:+UseG1GC -XX:+ClassUnloadingWithConcurrentMark -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof -XX:+UseCompressedOops -XX:+ExitOnOutOfMemoryError"

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    apk del tzdata

COPY --from=builder /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar
EXPOSE 80

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Duser.timezone=Asia/Seoul -jar app.jar --server.port=80 --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]