package com.efficientcapital.quickstart.vertx.web;

import com.efficientcapital.commons.http.response.MediaTypes;
import com.efficientcapital.commons.vertx.handler.http.ErrorHandler;
import com.efficientcapital.commons.vertx.handler.http.HealthCheckHandler;
import com.efficientcapital.commons.vertx.handler.http.ResourceNotFoundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpVerticle.class);

  private final ServiceDiscovery discovery;

  public HttpVerticle(ServiceDiscovery discovery) {
    this.discovery = discovery;
  }

  @Override
  public void start(Promise<Void> startFuture) {
    LOGGER.debug("HttpVerticle.start(..)");

    Router router = Router.router(vertx);

    // CORS and BodyHandler
    router.route()
      .handler(CorsHandler.create("*")
        .allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.PUT)
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedHeader(HttpHeaders.ACCEPT.toString())
        .allowedHeader(HttpHeaders.CONTENT_TYPE.toString()))
      .handler(BodyHandler.create());

    // Mount Handlers
    router.mountSubRouter("/", rootRouter(vertx));
    router.mountSubRouter("/health", healthRouter(vertx));
    router.mountSubRouter("/hello", simpleServiceRouter(vertx, SimpleServiceHandler.create(vertx, discovery)));

    // ResourceNotFoundHandler and Failurehandler
    router.route()
      .failureHandler(new ErrorHandler())
      .handler(new ResourceNotFoundHandler());

    // Start Server
    startHttpServer(vertx, startFuture, router, config().getString("serviceName"), config().getJsonObject("http"));
  }

  private void startHttpServer(Vertx vertx, Promise<Void> startFuture, Router router,
                               String serviceName, JsonObject httpSettings) {
    String host = httpSettings.getString("host");
    Integer port = httpSettings.getInteger("port", 0);
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port,
        result -> {
          if (result.succeeded()) {
            LOGGER.debug("Server running: \nHost: {} \nPort: {}", host, port);
            if (config().containsKey("KUBERNETES_NAMESPACE")) {
              startFuture.complete();
              return;
            } else {
              Record record = new Record()
                .setName(config().getString("serviceName"))
                .setLocation(
                  new JsonObject()
                    .put("host", host)
                    .put("port", port));
              LOGGER.debug("Adding a http service record into the service discovery backend. " +
                "\nRecord details: \nname: {} \nlocation: {}", record.getName(), record.getLocation());
              discovery.publish(record, discoverResult -> {
                if (discoverResult.succeeded()) {
                  LOGGER.debug("http service ({}) record has been published to the service registry",
                    config().getString("serviceName"));
                  startFuture.complete();
                  return;
                }
                LOGGER.error("Can't register the record with the service registry.", discoverResult.cause());
                startFuture.fail(discoverResult.cause());
              });
              return;
            }
          }
          LOGGER.error("Could bind the service ({}): {}", serviceName, result.cause());
          startFuture.fail(result.cause());
        });
  }

  private Router healthRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    router.get("/")
      .handler(new HealthCheckHandler())
      .failureHandler(new ErrorHandler());

    return router;
  }

  private Router rootRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    router.get("/")
      .handler(this::defaultHandler)
      .failureHandler(new ErrorHandler());

    return router;
  }

  private void defaultHandler(RoutingContext context) {
    LOGGER.debug("Executing the defaultHandler");
    JsonObject response = new JsonObject();
    response.put("status", HttpResponseStatus.OK.reasonPhrase());
    response.put("message", "Jenkins X! works like charm... sometimes");
    context.response()
      .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_JSON)
      .setStatusCode(HttpResponseStatus.OK.code())
      .end(response.encode());
  }

  private Router simpleServiceRouter(Vertx vertx, SimpleServiceHandler simpleServiceHandler) {
    Router router = Router.router(vertx);

    router.post("/")
      .handler(simpleServiceHandler::handleSayHello)
      .failureHandler(new ErrorHandler());

    return router;
  }
}
