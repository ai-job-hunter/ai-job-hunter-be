FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制构建文件
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

# 下载依赖
RUN ./gradlew dependencies --no-daemon

# 复制源代码
COPY src src

# 构建
RUN ./gradlew bootJar --no-daemon

# 运行镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Portainer / 应用日志用北京时间（compose 不必再写 TZ）
RUN apk add --no-cache tzdata
ENV TZ=Asia/Shanghai

# 复制构建产物
COPY --from=builder /app/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查（Alpine BusyBox wget；用 127.0.0.1 避免 localhost→::1）
# /actuator/health 必须在 SecurityConfig 里 permitAll，否则 403 → unhealthy
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

# JVM 参数只在这里维护；容器硬顶用 compose mem_limit（建议 512m~768m）
# MaxRAMPercentage 相对于容器可见内存（有 mem_limit 时才有意义）
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=15.0 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC"

# 启动（exec 保证信号正确；勿去掉 sh -c，否则 JAVA_OPTS 不会展开）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
