FROM gradle:jdk11 as builder
COPY --chown=gradle:gradle . /server
WORKDIR /server
RUN gradle shadowJar

FROM openjdk:8-alpine
WORKDIR /server
COPY --from=builder /server/build/libs .
EXPOSE 4567 8080
CMD ["java", "-jar", "server-1.0-SNAPSHOT-all.jar"]
