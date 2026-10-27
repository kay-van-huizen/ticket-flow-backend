# ---- Build-stage: JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
    && cp target/*.jar app.jar

# ---- Runtime-stage: JRE ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S ticketflow \
    && adduser -S -G ticketflow ticketflow \
    && mkdir -p /app/uploads \
    && chown ticketflow:ticketflow /app/uploads

COPY --from=build /build/app.jar app.jar

USER ticketflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
