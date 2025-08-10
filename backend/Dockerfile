FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/quarkus-app/ /app/
COPY wait-for-it.sh /app/
RUN chmod +x /app/wait-for-it.sh

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/quarkus-run.jar"]
