FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="AI-Health-System Team"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q "UP" || exit 1
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar