FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

LABEL authors="DungNQ"
LABEL description="Item Service - Gateway Offloading Pattern"

WORKDIR /build

COPY pom.xml .

COPY spring-user-service/pom.xml ./spring-user-service/
COPY spring-item-service/pom.xml ./spring-item-service/
COPY spring-order-service/pom.xml ./spring-order-service/

RUN mvn dependency:go-offline -B

COPY spring-item-service/src ./spring-item-service/src

RUN mvn clean package -pl spring-item-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

LABEL description="Item Service - Gateway Offloading Pattern"

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/spring-item-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
