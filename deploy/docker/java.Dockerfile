# Build the jar first: mvn -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-*.jar /app/app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
