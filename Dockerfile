# Stage 1 — Build
# Uses Maven + Java 17 to compile and package the app
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first — Docker caches this layer
# So dependencies only re-download when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2 — Run
# Smaller image — only needs JRE not full Maven
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]