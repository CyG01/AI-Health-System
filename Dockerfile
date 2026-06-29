FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="AI-Health-System Team"
RUN apk add --no-cache wget
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar