# ---- Build stage ----
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---- Serve stage ----
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# busybox wget(--spider)으로 정적 index.html 서빙 가능 여부 확인.
# nginx가 SIGSEGV 등으로 죽으면 즉시 unhealthy로 표시되어 docker compose가 재시작 결정에 사용 가능.
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
