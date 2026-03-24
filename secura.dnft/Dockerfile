# ---------- STAGE 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy project files
COPY secura.dnft/pom.xml .
COPY secura.dnft/src ./src

# Build jar
RUN mvn clean package -DskipTests


# ---------- STAGE 2: Run ----------
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Railway uses PORT env variable
ENV PORT=8080

EXPOSE 8080

# Run app
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT"]