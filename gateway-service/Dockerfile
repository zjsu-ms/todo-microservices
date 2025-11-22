FROM eclipse-temurin:25-jre

WORKDIR /app

# 复制JAR文件
COPY target/gateway-service-2.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
