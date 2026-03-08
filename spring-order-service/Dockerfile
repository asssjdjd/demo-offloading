FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

LABEL authors="LongPT"
LABEL description="Order Service - Gateway Offloading Pattern"

WORKDIR /build

COPY pom.xml .

COPY spring-user-service/pom.xml ./spring-user-service/
COPY spring-item-service/pom.xml ./spring-item-service/
COPY spring-order-service/pom.xml ./spring-order-service/

RUN mvn dependency:go-offline -B

COPY spring-order-service/src ./spring-order-service/src

RUN mvn clean package -pl spring-order-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/spring-order-service/target/*.jar app.jar
EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]