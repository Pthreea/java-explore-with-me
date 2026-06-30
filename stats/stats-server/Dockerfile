FROM amazoncorretto:21-alpine-jdk

# Установка curl для healthcheck
RUN apk add --no-cache curl

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]