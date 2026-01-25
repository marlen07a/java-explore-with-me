FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /workspace
COPY . .
RUN mvn -pl stats/stats-server -am package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /workspace/stats/stats-server/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
