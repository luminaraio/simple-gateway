package com.efficientcapital.quickstart.vertx.web;

import com.efficientcapital.commons.http.response.MediaTypes;
import com.efficientcapital.commons.vertx.handler.grpc.GrpcHandler;
import com.efficientcapital.quickstart.vertx.GreeterGrpc;
import com.efficientcapital.quickstart.vertx.HelloReply;
import com.efficientcapital.quickstart.vertx.HelloRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Luminara Team.
 */
public class SimpleServiceHandler implements GrpcHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServiceHandler.class);
  private static final String SERVICE_NAME = "simple-service";

  private final Vertx vertx;
  private final ServiceDiscovery discovery;
  private final JsonObject serviceFilter;

  private SimpleServiceHandler(Vertx vertx, ServiceDiscovery discovery) {
    this.vertx = vertx;
    this.discovery = discovery;
    this.serviceFilter = new JsonObject()
      .put("name", SERVICE_NAME);
  }

  public static SimpleServiceHandler create(Vertx vertx, ServiceDiscovery discovery) {
    return new SimpleServiceHandler(vertx, discovery);
  }

  public void handleSayHello(RoutingContext routingContext) {
    LOGGER.debug("In SimpleServiceHandler.handleSayHello(..)");
    getStub(vertx, discovery, serviceFilter, GreeterGrpc::newVertxStub,
      proxyAsyncResult -> sayHello(routingContext, proxyAsyncResult));
  }

  private void sayHello(RoutingContext routingContext, AsyncResult<GreeterGrpc.GreeterVertxStub> proxyAsyncResult) {
    LOGGER.debug("In SimpleServiceHandler.sayHello(..)");
    if (proxyAsyncResult.succeeded()) {
      LOGGER.debug("In SimpleServiceHandler.sayHello(..) => getting proxy service");
      GreeterGrpc.GreeterVertxStub service = proxyAsyncResult.result();
      LOGGER.debug("In SimpleServiceHandler.sayHello(..) => retrieved proxy service");
      JsonObject payload = routingContext.getBodyAsJson();
      LOGGER.debug("In SimpleServiceHandler.sayHello(..) => read request payload");
      LOGGER.debug("In SimpleServiceHandler.sayHello(..) => build gRPC request");
      service.sayHello(HelloRequest.newBuilder()
        .setName(payload.getString("name"))
        .build(), serviceResult ->
        processServiceResponse(routingContext, HttpResponseStatus.OK, serviceResult));
    } else {
      LOGGER.error("ERROR while proxying request", proxyAsyncResult.cause());
      routingContext.fail(proxyAsyncResult.cause());
    }
  }

  private void processServiceResponse(RoutingContext routingContext, HttpResponseStatus httpStatus,
                                      AsyncResult<HelloReply> serviceResult) {
    LOGGER.debug("In SimpleServiceHandler.processServiceResponse(..)");
    if (serviceResult.succeeded()) {
      routingContext
        .response()
        .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_JSON)
        .setStatusCode(httpStatus.code())
        .end(new JsonObject().put("message", serviceResult.result().getMessage()).encode());
    } else {
      processServiceError(routingContext, serviceResult);
    }
  }
}
