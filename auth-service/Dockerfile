FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S codepilot && adduser -S codepilot -G codepilot
USER codepilot

COPY target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]