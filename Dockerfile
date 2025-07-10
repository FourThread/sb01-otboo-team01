FROM gradle:7.6.0-jdk17 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

COPY gradlew ./
RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:17-jdk-jammy

ENV PROJECT_NAME=duckhu
ENV PROJECT_VERSION=1.2-M8
ENV JVM_OPTS=""

WORKDIR /app

COPY --from=builder /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh","-c","java -Duser.timezone=Asia/Seoul $JVM_OPTS -jar app.jar"]