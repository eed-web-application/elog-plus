FROM eclipse-temurin:19-jdk-jammy
RUN useradd -rm -d /home/app -s /bin/bash -g root -G sudo -u 1001 app
USER app
WORKDIR /home/app
COPY /tools/run.sh /home/app
COPY build/libs/eed-java-backend-example-*.jar /home/app/app.jar
EXPOSE 8080
ENTRYPOINT ["/home/app/run.sh"]