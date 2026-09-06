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

# 复制构建产物
COPY --from=builder /app/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]
