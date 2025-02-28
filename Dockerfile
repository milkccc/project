# 使用官方 OpenJDK 11 镜像作为基础镜像
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app/pollux

# 声明宿主机模型挂载路径环境变量
ENV MODEL_HOST_DIR=/media/nfs/pollux/model

# 声明共享卷名称
ENV DATA_VOLUME="pollux-"

# 将 Spring Boot JAR 文件复制到工作目录
COPY target/pollux-backend.jar pollux-backend.jar

# 将 application.yml 配置文件复制到工作目录
COPY src/main/resources/application.yml application.yml

# 暴露应用程序端口
EXPOSE 7529

# 启动 Spring Boot 应用
CMD ["java", "-jar", "pollux-backend.jar", "--spring.config.location=/app/pollux/application.yml"]