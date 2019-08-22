FROM adoptopenjdk/openjdk11:alpine-jre

MAINTAINER Luminara Team <tech@luminara.io>

# Set the location of the verticles
ENV VERTX_HOME /usr/local/vertx

EXPOSE 8080

# Launch the verticle
WORKDIR $VERTX_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar vertx-app.jar -cluster"]

# Copy your fat jar and the config file to the to the container
COPY target/simple-gateway-*.jar $VERTX_HOME/vertx-app.jar