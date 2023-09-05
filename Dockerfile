# Phase 1: Build the Spring application using official Gradle image and Java 17
FROM gradle:8-jdk19 AS build

WORKDIR /opt/app
# Copy only the Gradle files to leverage Docker layer caching
COPY . ./

# Build the application
RUN gradle assemble --no-daemon
# Phase 2: Create the final image with the built artifact
FROM eclipse-temurin:19-jdk-jammy

WORKDIR /app

# Copy the built artifact from the previous phase
COPY --from=build /opt/app/tools/run.sh .
COPY --from=build /opt/app/build/libs/*.jar ./app.jar
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /app
RUN chmod a+x /app/wait
RUN chmod a+x /app/run.sh
# Set the entrypoint to run the Spring application
ENTRYPOINT ["./run.sh"]