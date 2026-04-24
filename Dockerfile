FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

RUN useradd -r -u 1001 appuser && chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
