# Use Java 11
FROM openjdk:11-jre-slim

# Owner
MAINTAINER alperenp

#                                                       (1)
ENV VERTICLE_FILE target/sesame-challenge-alperenp-1.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Copy your verticle to the container                   (2)
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Copy your config file to the container
COPY src/main/resources $VERTICLE_HOME/conf

# Launch the verticle                                   (3)
WORKDIR $VERTICLE_HOME

# https://docs.openshift.org/latest/creating_images/guidelines.html#use-uid
#RUN chgrp -R 0 $VERTICLE_HOME && \
#    chmod -R g=u $VERTICLE_HOME


CMD java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dlogback.configurationFile=conf/logback.xml -Dvertx.disableFileCPResolving=true -jar $VERTICLE_HOME/$VERTICLE_FILE -conf conf/config.json