# Cloud Run image for the fulfilment role (Pub/Sub push -> Cloud SQL, receipts -> Cloud Storage).
# Builds the JAR inside Docker, so "gcloud run deploy --source ." works without a local build.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/gcp-flash-sale-system-0.0.1.jar app.jar
ENV SPRING_PROFILES_ACTIVE=fulfilment
ENTRYPOINT ["java", "-jar", "/app/app.jar"]