# Build
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY tchalanet-common/pom.xml tchalanet-common/pom.xml
COPY tchalanet-catalog/pom.xml tchalanet-catalog/pom.xml
COPY tchalanet-platform/pom.xml tchalanet-platform/pom.xml
COPY tchalanet-core/pom.xml tchalanet-core/pom.xml
COPY tchalanet-features/pom.xml tchalanet-features/pom.xml
COPY tchalanet-app/pom.xml tchalanet-app/pom.xml
RUN mvn -q -e -DskipTests -pl tchalanet-app -am dependency:go-offline

COPY tchalanet-common/src tchalanet-common/src
COPY tchalanet-catalog/src tchalanet-catalog/src
COPY tchalanet-platform/src tchalanet-platform/src
COPY tchalanet-core/src tchalanet-core/src
COPY tchalanet-features/src tchalanet-features/src
COPY tchalanet-app/src tchalanet-app/src
RUN mvn -q -DskipTests -pl tchalanet-app -am package

# Run
FROM eclipse-temurin:25-jre-alpine-3.23
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
WORKDIR /srv
COPY --from=build /app/tchalanet-app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
