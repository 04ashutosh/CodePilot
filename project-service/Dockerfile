FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/project-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]