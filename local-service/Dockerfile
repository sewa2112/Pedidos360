# ===========================
# STAGE 1: Build con Maven
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn clean package -DskipTests -B

# ===========================
# STAGE 2: Imagen de ejecucion
# ===========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

ENV SERVER_PORT=8082

ENTRYPOINT ["java", "-jar", "app.jar"]
