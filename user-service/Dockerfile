FROM eclipse-temurin:25-jre

WORKDIR /app

# 复制JAR文件
COPY target/user-service-1.2.0.jar app.jar

# 暴露端口
EXPOSE 8081

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
