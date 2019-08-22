package com.efficientcapital.quickstart.vertx.web;

import com.efficientcapital.commons.vertx.verticle.AbstractMainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractMainVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  protected void deployVerticles(JsonObject config) {
    // Deploy Http Verticle
    vertx.deployVerticle(new HttpVerticle(discovery),
      new DeploymentOptions()
        .setConfig(config));
  }
}
