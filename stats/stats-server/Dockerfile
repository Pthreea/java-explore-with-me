FROM amazoncorretto:21-alpine-jdk

RUN apk add --no-cache curl

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9090

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=10 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]