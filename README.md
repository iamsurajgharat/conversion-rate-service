# conversion-rate-service

## Database Migration

```bash
sbt flywayMigrate

## Compile test code in sbt

test:compile

## Run some test cases

testOnly com.surajgharat.conversionrates.*

## psql command for local dockerized postgresql

psql -h localhost -p 5432 -U postgres

## generate docker image
sbt > docker:publishLocal

## run generated docker image
docker run --rm -p 9000:9000 --env APPLICATION_SECRET=abcdefghijklmnopqrstuvwxyz conversion-rate-service:1.0-SNAPSHOT 
