package com.efficientcapital.quickstart.vertx.web;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class HttpVerticleTest {

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext testContext) {
    JsonObject config = new JsonObject()
      .put("serviceName", "simple-gateway")
      .put("http", new JsonObject()
        .put("host", "localhost")
        .put("port", 8080));

    ServiceDiscovery discovery = ServiceDiscovery.create(
      vertx, new ServiceDiscoveryOptions().setBackendConfiguration(config));

    vertx.deployVerticle(new HttpVerticle(discovery), new DeploymentOptions().setConfig(config),
      testContext.succeeding(id -> testContext.completeNow()));
  }

  @AfterEach
  @DisplayName("Check that the verticles is still there")
  void lastChecks(Vertx vertx) {
    assertThat(vertx.deploymentIDs().isEmpty(), is(equalTo(false)));
    assertThat(vertx.deploymentIDs(), hasSize(1));
  }

  @Test
  @DisplayName("Check that the server has started")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void checkServerHasStarted(Vertx vertx, VertxTestContext testContext) {
    // GIVEN
    WebClient webClient = WebClient.create(vertx);

    // WHEN
    webClient.get(8080, "localhost", "/")
      .as(BodyCodec.jsonObject())
      .send(testContext.succeeding(getResponse ->
        testContext.verify(() -> {
          JsonObject response = getResponse.body();
          // THEN
          assertEquals(200, getResponse.statusCode());
          assertThat("Response does not have message field",
            response.containsKey("message"), is(true));
          assertThat("Response does not contain the expected value",
            response.getString("message"), containsString("Jenkins X!"));
          testContext.completeNow();
        })));
  }
}
