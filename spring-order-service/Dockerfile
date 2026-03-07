FROM eclipse-temurin:17-jre-alpine

LABEL description="Order Service - Gateway Offloading Pattern"

WORKDIR /app

RUN apk add --no-cache curl

COPY target/spring-order-service-0.0.1-SNAPSHOT.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
