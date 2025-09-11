FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew ./gradlew
RUN chmod +x ./gradlew
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY module-common/ ./module-common

# 실행 가능한 bootJar 생성
RUN ./gradlew :module-common:bootJar --no-daemon --build-cache

# 실행 단계
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/module-common/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
