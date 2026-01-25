FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -pl main-service -am package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /workspace/main-service/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
