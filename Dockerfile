# Base layer with dependencies
FROM openjdk:17-alpine as base
WORKDIR /app
COPY gradle/ ./gradle/
COPY build.gradle settings.gradle ./
COPY gradlew gradlew.bat ./
RUN ./gradlew dependencies

# Layer with application code
FROM base as code
COPY src/ ./src/
RUN ./gradlew build && ls -la build/libs

# Final layer with runtime environment
FROM openjdk:17-alpine
WORKDIR /app
COPY --from=code /app/build/libs/*.jar app.jar

# Delay before application launch
ENTRYPOINT ["sh", "-c", "sleep 120 && java -jar app.jar"]