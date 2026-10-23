# ---- build stage: compile the jar (tests run in CI, not here) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package && cp target/*.jar app.jar

# ---- runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --no-create-home ticketflow
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER ticketflow
EXPOSE 8080
# Fit comfortably in a 512 MB free-tier instance; PORT is honoured via server.port=${PORT:8080}.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "app.jar"]
