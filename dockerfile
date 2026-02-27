#---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -m -u 10001 appuser
USER appuser

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080 9101

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
