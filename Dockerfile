# ---- Build stage ----
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# PDF 한글 렌더링을 위한 나눔 폰트 설치
RUN apt-get update && apt-get install -y --no-install-recommends fonts-nanum \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
