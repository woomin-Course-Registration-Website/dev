# ---- Build stage ----
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# PDF 한글 렌더링을 위한 나눔 폰트와 헬스체크용 curl 설치
RUN apt-get update && apt-get install -y --no-install-recommends fonts-nanum curl \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# 로컬 docker run / docker compose에서 Spring Boot 부팅 완료 가시화.
# K8s 환경에서는 deployment.yaml의 readiness/liveness probe가 별도 사용됨.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
