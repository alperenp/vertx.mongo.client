# vertx.mongo.client
This repository contains example of a vertx service using vertx client and its tests

# Requirements
-  Java 10+ (Tested using jdk 11)
-  Apache Maven
-  Either Docker or MongoDB running on machine

# Build
`mvn clean install -DskipTests` or if your machine has installation of mongo `mvn clean install`

# Run
Option 1
1.  `docker-compose up

Option 2
1.  Configure config file (located under src/main/resources/config.json)
1.  Under target directory, you will find fat-jar to run if you would like to run the service on your localhost (assuming you have mongo installed)

Option 3 (to run service in docker container without mongo)
1.  `docker build -t alperenp-service .`
2.  `docker run -p 8080:8080 alperenp-service`
