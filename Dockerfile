FROM eclipse-temurin:17-jre-alpine

COPY target/ai-0.0.1-SNAPSHOT.jar app.jar

ENV TZ Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
