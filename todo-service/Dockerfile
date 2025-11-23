FROM eclipse-temurin:25-jre
WORKDIR /app

# 复制已构建的JAR文件
COPY target/todo-service-2.2.0.jar app.jar

# 暴露端口
EXPOSE 8082

# JVM参数优化
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
