package co.edu.escuelaing.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.escuelaing.Exceptions;
import co.edu.escuelaing.Order;
import co.edu.escuelaing.OrderRequest;
import co.edu.escuelaing.OrderService;

/**
 * HTTP adapter between API Gateway (Lambda proxy integration) and OrderService.
 * It is the only class that knows about HTTP: routing, parameters, status codes,
 * headers and JSON bodies. OrderService stays plain Java.
 */
public class OrdersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OrderService service;

    public OrdersHandler() {
        this(new OrderService());
    }

    public OrdersHandler(OrderService service) {
        this.service = service;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            return route(event);
        } catch (Exceptions e) {
            return error(statusFor(e), e.getExceptions().name(), e.getDetail());
        } catch (JsonProcessingException e) {
            return error(400, "INVALID_REQUEST", "Request body is not valid JSON for an order");
        } catch (RuntimeException e) {
            if (context != null) {
                context.getLogger().log("Unexpected error: " + e);
            }
            return error(500, "INTERNAL_ERROR", "Unexpected error while processing the request");
        }
    }

    private APIGatewayProxyResponseEvent route(APIGatewayProxyRequestEvent event)
            throws Exceptions, JsonProcessingException {
        String method = event.getHttpMethod();
        String[] segments = segments(event.getPath());

        if (segments.length == 1 && segments[0].equals("orders")) {
            if ("POST".equals(method)) {
                return createOrder(event);
            }
            if ("GET".equals(method)) {
                return findOrdersByCustomer(event);
            }
            return error(405, "METHOD_NOT_ALLOWED", method + " is not allowed on /orders");
        }

        if (segments.length == 2 && segments[0].equals("orders")) {
            String id = pathParameter(event, "id", segments[1]);
            if ("GET".equals(method)) {
                return getOrder(id);
            }
            if ("DELETE".equals(method)) {
                return deleteOrder(id);
            }
            return error(405, "METHOD_NOT_ALLOWED", method + " is not allowed on /orders/{id}");
        }

        return error(404, "NOT_FOUND", "Route " + event.getPath() + " does not exist");
    }

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event)
            throws Exceptions, JsonProcessingException {
        OrderRequest request = event.getBody() == null
                ? null
                : MAPPER.readValue(event.getBody(), OrderRequest.class);

        Order order = service.createOrder(request);

        APIGatewayProxyResponseEvent response = json(201, order);
        response.getHeaders().put("Location", requestPath(event) + "/" + order.getId());
        return response;
    }

    // Path as the client used it, including the stage (e.g. /dev/orders), so Location resolves correctly
    private static String requestPath(APIGatewayProxyRequestEvent event) {
        APIGatewayProxyRequestEvent.ProxyRequestContext context = event.getRequestContext();
        String path = context != null && context.getPath() != null ? context.getPath() : event.getPath();
        return path.replaceAll("/+$", "");
    }

    private APIGatewayProxyResponseEvent getOrder(String id) throws Exceptions, JsonProcessingException {
        return json(200, service.getOrder(id));
    }

    private APIGatewayProxyResponseEvent deleteOrder(String id) throws Exceptions {
        service.deleteOrder(id);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(204)
                .withHeaders(new LinkedHashMap<>());
    }

    private APIGatewayProxyResponseEvent findOrdersByCustomer(APIGatewayProxyRequestEvent event)
            throws Exceptions, JsonProcessingException {
        Map<String, String> query = event.getQueryStringParameters();
        String customerId = query == null ? null : query.get("customerId");

        List<Order> orders = service.getOrdersByCustomer(customerId);
        return json(200, orders);
    }

    private static int statusFor(Exceptions e) {
        switch (e.getExceptions()) {
            case ORDER_NOT_FOUND:
                return 404;
            default:
                return 400;
        }
    }

    private static String pathParameter(APIGatewayProxyRequestEvent event, String name, String fallback) {
        Map<String, String> params = event.getPathParameters();
        if (params != null && params.get(name) != null) {
            return params.get(name);
        }
        return fallback;
    }

    private static String[] segments(String path) {
        if (path == null) {
            return new String[0];
        }
        String trimmed = path.replaceAll("^/+|/+$", "");
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/");
    }

    private static APIGatewayProxyResponseEvent json(int status, Object body) throws JsonProcessingException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(headers)
                .withBody(MAPPER.writeValueAsString(body));
    }

    private static APIGatewayProxyResponseEvent error(int status, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        try {
            return json(status, body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize error response", e);
        }
    }
}
