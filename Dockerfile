# Etapa 1: Construcción
# Usamos una imagen oficial que ya tiene Maven y Java integrados
FROM maven:3.9.6-eclipse-temurin-17 AS buildstage

WORKDIR /app

# Descargar dependencias primero (mejora la velocidad de compilaciones futuras usando caché)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código y compilar
COPY src /app/src
RUN mvn clean package -DskipTests

# ---------------------------------------------------

# Etapa 2: Ejecución
# Usamos la versión JRE (Runtime) para que la imagen final sea mucho más liviana
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR compilado de la etapa anterior y renombrarlo de forma genérica
COPY --from=buildstage /app/target/*.jar /app/app.jar

# Exponer puerto (8080 es el estándar de Spring Boot, cámbialo si usas otro)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]