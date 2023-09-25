# ELOG-plus

Java backend for the management of ELOG management

## Execute test for development
docker compose -f docker-compose.yml -up
./gradlew clean test

## Execute application locally using docker compose
docker compose -f docker-compose.yml -f docker-compose-app.yml up --build

## JWT Application token key
the ***app-token-jwt-key*** properties can be filled using the openssl tools like shown below
```shell
openssl rand -hex <size> 
```
