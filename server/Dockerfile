FROM gradle:jdk11 as builder
COPY --chown=gradle:gradle . /server
WORKDIR /server
RUN gradle shadowJar

FROM openjdk:12-alpine
WORKDIR /server
COPY --from=builder /server/build/libs .
EXPOSE 4567
CMD ["java", "-jar", "server-1.0-SNAPSHOT-all.jar"]
