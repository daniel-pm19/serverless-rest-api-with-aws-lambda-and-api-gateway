package co.edu.escuelaing.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import co.edu.escuelaing.Order;
import co.edu.escuelaing.OrderService;

class OrdersHandlerTest {

    private OrdersHandler handler;

    @BeforeEach
    void setUp() {
        // default OrderService: ORD-42, ORD-43 (CUST-42) and ORD-44 (CUST-7)
        handler = new OrdersHandler();
    }

    private APIGatewayProxyResponseEvent call(String method, String path, Map<String, String> pathParams,
            Map<String, String> query, String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod(method)
                .withPath(path)
                .withPathParameters(pathParams)
                .withQueryStringParameters(query)
                .withBody(body);
        return handler.handleRequest(event, null);
    }

    // GET /orders/{id}

    @Test
    void shouldReturn200ForExistingOrder() {
        APIGatewayProxyResponseEvent response = call("GET", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("{\"id\":\"ORD-42\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                response.getBody());
    }

    @Test
    void shouldReturn404ForMissingOrder() {
        APIGatewayProxyResponseEvent response = call("GET", "/orders/ORD-999", Map.of("id", "ORD-999"), null, null);

        assertEquals(404, response.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.getBody());
    }

    // GET /orders?customerId=

    @Test
    void shouldReturnOrdersOfCustomer() {
        APIGatewayProxyResponseEvent response = call("GET", "/orders", null, Map.of("customerId", "CUST-42"), null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("ORD-42"));
        assertTrue(response.getBody().contains("ORD-43"));
    }

    @Test
    void shouldReturnEmptyArrayForCustomerWithoutOrders() {
        APIGatewayProxyResponseEvent response = call("GET", "/orders", null, Map.of("customerId", "CUST-100"), null);

        assertEquals(200, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    void shouldReturn400WhenCustomerIdIsMissing() {
        APIGatewayProxyResponseEvent response = call("GET", "/orders", null, null, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"INVALID_CUSTOMER\""));
    }

    // POST /orders

    @Test
    void shouldReturn201WithLocationWhenOrderIsCreated() {
        APIGatewayProxyResponseEvent response = call("POST", "/orders", null, null,
                "{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}");

        assertEquals(201, response.getStatusCode());
        assertEquals("/orders/ORD-4", response.getHeaders().get("Location"));
        assertEquals("{\"id\":\"ORD-4\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                response.getBody());
    }

    @Test
    void locationShouldIncludeTheStageWhenApiGatewayProvidesIt() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withPath("/orders")
                .withRequestContext(new APIGatewayProxyRequestEvent.ProxyRequestContext().withPath("/dev/orders"))
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(201, response.getStatusCode());
        assertEquals("/dev/orders/ORD-4", response.getHeaders().get("Location"));
    }

    @Test
    void createdOrderCanBeRetrievedThroughTheSameHandler() {
        call("POST", "/orders", null, null, "{\"customerId\":\"CUST-9\",\"product\":\"iPad\",\"quantity\":3}");

        APIGatewayProxyResponseEvent response = call("GET", "/orders/ORD-4", Map.of("id", "ORD-4"), null, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"customerId\":\"CUST-9\""));
    }

    @Test
    void shouldReturn400ForInvalidQuantity() {
        APIGatewayProxyResponseEvent response = call("POST", "/orders", null, null,
                "{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":0}");

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"INVALID_QUANTITY\",\"message\":\"Quantity must be > 0\"}", response.getBody());
    }

    @Test
    void shouldReturn400ForMalformedJson() {
        APIGatewayProxyResponseEvent response = call("POST", "/orders", null, null, "{malformed");

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"INVALID_REQUEST\""));
    }

    @Test
    void shouldReturn400ForWrongFieldType() {
        APIGatewayProxyResponseEvent response = call("POST", "/orders", null, null,
                "{\"customerId\":\"CUST-42\",\"product\":\"iPad\",\"quantity\":\"two\"}");

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"INVALID_REQUEST\""));
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() {
        APIGatewayProxyResponseEvent response = call("POST", "/orders", null, null, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"INVALID_ORDER\""));
    }

    // DELETE /orders/{id}

    @Test
    void shouldReturn204WithoutBodyWhenOrderIsDeleted() {
        APIGatewayProxyResponseEvent response = call("DELETE", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentOrder() {
        APIGatewayProxyResponseEvent response = call("DELETE", "/orders/ORD-999", Map.of("id", "ORD-999"), null, null);

        assertEquals(404, response.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.getBody());
    }

    @Test
    void deletedOrderCannotBeRetrieved() {
        call("DELETE", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        APIGatewayProxyResponseEvent response = call("GET", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        assertEquals(404, response.getStatusCode());
    }

    @Test
    void deletingTwiceReturns404TheSecondTime() {
        call("DELETE", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        APIGatewayProxyResponseEvent response = call("DELETE", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        assertEquals(404, response.getStatusCode());
    }

    // unexpected internal error

    @Test
    void shouldReturn500WithoutInternalDetailsOnUnexpectedError() {
        OrderService broken = new OrderService() {
            @Override
            public Order getOrder(String id) {
                throw new IllegalStateException("database connection lost at OrderRepository.java:87");
            }
        };
        OrdersHandler brokenHandler = new OrdersHandler(broken);
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withPath("/orders/ORD-42")
                .withPathParameters(Map.of("id", "ORD-42"));

        APIGatewayProxyResponseEvent response = brokenHandler.handleRequest(event, null);

        assertEquals(500, response.getStatusCode());
        assertEquals("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Unexpected error while processing the request\"}",
                response.getBody());
    }

    // routing

    @Test
    void shouldReturn405ForUnsupportedMethod() {
        APIGatewayProxyResponseEvent response = call("PUT", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"METHOD_NOT_ALLOWED\""));
    }

    @Test
    void shouldReturn404ForUnknownRoute() {
        APIGatewayProxyResponseEvent response = call("GET", "/customers", null, null, null);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"error\":\"NOT_FOUND\""));
    }
}
