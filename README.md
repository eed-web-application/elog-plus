# elog-plus
Java backend for the management of elog

## Execute test
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from test

### for view only the logging form the test:
docker compose -f docker-compose.yml -f docker-compose.test.yml up --attach test  --exit-code-from test
