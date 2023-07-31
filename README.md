# elog-plus

Java backend for the management of elog

## Execute test fro development
docker compose -f docker-compose.yml -up
./gradlew clean test

## Execute application locally usign docker compose
docker compose -f docker-compose.yml -f docker-compose-app.yml up --build
