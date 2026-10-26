package co.edu.escuelaing.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

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

    // routing

    @Test
    void shouldReturn405ForUnsupportedMethod() {
        APIGatewayProxyResponseEvent response = call("DELETE", "/orders/ORD-42", Map.of("id", "ORD-42"), null, null);

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
