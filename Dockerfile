# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
    && cp "$(ls target/url-shortener-*.jar | grep -v '\.original$' | head -n1)" /app/app.jar

# Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/app.jar ./app.jar
COPY public ./public

ENV SERVER_PORT=8080
# For Managed Postgres on the Droplet, set:
#   SPRING_PROFILES_ACTIVE=prod
#   SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:25060/defaultdb?sslmode=require
#   SPRING_DATASOURCE_USERNAME=...
#   SPRING_DATASOURCE_PASSWORD=...
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
