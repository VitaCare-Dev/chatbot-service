# Etapa 1: Construcción
FROM maven:3.9.6-eclipse-temurin-21 AS buildstage

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src /app/src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=buildstage /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]