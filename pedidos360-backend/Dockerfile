# ===========================
# STAGE 1: Build con Maven
# ===========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar descriptor de dependencias primero para optimizar la caché de capas Docker
COPY pom.xml .

# Pre-descargar dependencias (permite compilaciones más rápidas si el pom.xml no cambia)
RUN mvn dependency:go-offline -B || true

# Copiar el código fuente y compilar el paquete omitiendo pruebas para agilizar el build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===========================
# STAGE 2: Imagen de ejecución (JRE ligera)
# ===========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el artefacto JAR generado desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto donde corre la aplicación Spring Boot
EXPOSE 8085

# Variables de entorno por defecto
ENV SERVER_PORT=8085

# Ejecución del backend
ENTRYPOINT ["java", "-jar", "app.jar"]
