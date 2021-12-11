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

