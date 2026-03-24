# ---------- BUILD ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY secura.dnft/pom.xml .
COPY secura.dnft/src ./src

RUN mvn clean package -DskipTests

# ---------- RUN ----------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=$PORT --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-railway}"]
