FROM gradle:5.6.4-jdk11 as builder
COPY --chown=gradle:gradle . /server
WORKDIR /server
RUN gradle bootJar -Pvaadin.productionMode

FROM openjdk:11-slim
WORKDIR /server
COPY --from=builder /server/build/libs .
EXPOSE 4567 8080
CMD ["java", "-jar", "server-1.0-SNAPSHOT.jar"]