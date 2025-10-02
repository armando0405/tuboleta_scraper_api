# Usa una imagen base de Java
FROM eclipse-temurin:21.0.7_6-jdk-alpine

# Información del mantenedor
LABEL maintainer="armando0405"

# Crear directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn/ .mvn/
COPY pom.xml .

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Descargar dependencias (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Construir la aplicación
RUN ./mvnw clean package -DskipTests

# Crear directorio para la base de datos H2
RUN mkdir -p /app/data

# Exponer el puerto 8080
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "target/tuboleta-scraper-0.0.1-SNAPSHOT.jar"]