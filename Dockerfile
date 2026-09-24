FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

VOLUME /tmp
EXPOSE 8080

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]

# docker build -t shopee-be .
# docker run -p 8080:8080 --env-file .env shopee-be
# docker stop <container_id_or_name>
