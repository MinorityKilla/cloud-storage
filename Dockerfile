FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/cloud-storage-1.0.0.jar app.jar
RUN mkdir -p /uploads
ENTRYPOINT ["java","-jar","/app.jar"]