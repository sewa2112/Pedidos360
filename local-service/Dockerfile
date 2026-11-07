# ===========================
# STAGE 1: Build con Maven
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app
ENV MAVEN_OPTS="-Xmx384m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===========================
# STAGE 2: Imagen de ejecucion ligera
# ===========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082
ENV SERVER_PORT=8082

ENTRYPOINT ["java", "-Xmx200m", "-jar", "app.jar"]
