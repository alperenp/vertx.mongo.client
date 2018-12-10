# Use Java 11
FROM openjdk:11-jre-slim

# Owner
MAINTAINER alperenp

# Install appointment service
# port to be used
EXPOSE 8080

ENV VERTICLE_FILE *-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

WORKDIR $VERTICLE_HOME

# Copy your verticle to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME

# Copy your config file to the container
COPY src/main/resources $VERTICLE_HOME/conf


CMD java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dlogback.configurationFile=conf/logback.xml -Dvertx.disableFileCPResolving=true -jar $VERTICLE_FILE -conf conf/config-docker-machine.json