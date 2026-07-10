FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S limitflow && adduser -S limitflow -G limitflow
COPY --from=build /workspace/target/backend-api-*.jar app.jar
USER limitflow

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
