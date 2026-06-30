FROM amazoncorretto:21-alpine-jdk

RUN apk add --no-cache curl

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9090

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]