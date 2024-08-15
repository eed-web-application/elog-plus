FROM eclipse-temurin:21-jammy
RUN apt-get update && \
    apt-get install -y ghostscript && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN useradd -rm -d /home/app -s /bin/bash -g root -G sudo -u 1001 app

WORKDIR /home/app
COPY ./tools/run.sh /home/app
COPY ./build/libs/elog-plus-backend-*-plain.jar /home/app/app-plain.jar
COPY ./build/libs/elog-plus-backend-*.jar /home/app/app.jar
RUN chown app:root /home/app/*.jar \
    && chmod 755 /home/app/*.jar

ENV WAIT_VERSION 2.7.2
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/$WAIT_VERSION/wait /home/app/wait
RUN chown app:root /home/app/wait \
    && chmod 755 /home/app/wait

# switch to non-root user
USER app
EXPOSE 8080
ENTRYPOINT /home/app/wait && exec java -jar /home/app/app.jar