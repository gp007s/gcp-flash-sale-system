# Cloud Run image for the fulfilment role (Pub/Sub push -> Cloud SQL, receipts -> Cloud Storage).
# JAR is copied from the build context (downloaded from GCS by CI before docker build).
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY app.jar app.jar
ENV SPRING_PROFILES_ACTIVE=fulfilment
ENTRYPOINT ["java", "-jar", "/app/app.jar"]