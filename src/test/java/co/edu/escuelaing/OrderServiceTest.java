package co.edu.escuelaing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.escuelaing.OrderException.ErrorType;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(new ArrayList<>());
    }

    // sample data (used by Lambda through the no-arg constructor)

    @Test
    void shouldLoadSampleOrdersWithDefaultConstructor() throws Exception {
        OrderService withSamples = new OrderService();

        Order order = withSamples.getOrder("ORD-42");

        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(2, withSamples.getOrdersByCustomer("CUST-42").size());
    }

    // createOrder

    @Test
    void shouldCreateValidOrder() throws Exception {
        Order order = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        assertNotNull(order.getId());
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void shouldGenerateSequentialUniqueIds() throws Exception {
        Order first = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));
        Order second = service.createOrder(new OrderRequest("CUST-42", "Magic Mouse", 2));

        assertEquals("ORD-1", first.getId());
        assertEquals("ORD-2", second.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    // getOrder

    @Test
    void shouldRetrieveExistingOrder() throws Exception {
        Order created = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        Order found = service.getOrder(created.getId());

        assertSame(created, found);
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrder("ORD-999"));

        assertEquals(ErrorType.ORDER_NOT_FOUND, ex.getType());
    }

    @Test
    void shouldPrefixErrorMessageWithErrorCode() {
        // API Gateway maps errors to HTTP status codes by matching this prefix
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrder("ORD-999"));

        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", ex.getMessage());
    }

    // deleteOrder

    @Test
    void shouldDeleteExistingOrder() throws Exception {
        Order created = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        service.deleteOrder(created.getId());

        OrderException ex = assertThrows(OrderException.class, () -> service.getOrder(created.getId()));
        assertEquals(ErrorType.ORDER_NOT_FOUND, ex.getType());
    }

    @Test
    void shouldFailWhenDeletingNonexistentOrder() {
        OrderException ex = assertThrows(OrderException.class, () -> service.deleteOrder("ORD-999"));

        assertEquals(ErrorType.ORDER_NOT_FOUND, ex.getType());
    }

    @Test
    void shouldRejectBlankIdWhenDeleting() {
        OrderException ex = assertThrows(OrderException.class, () -> service.deleteOrder(" "));

        assertEquals(ErrorType.INVALID_ORDER, ex.getType());
    }

    @Test
    void shouldOnlyDeleteTheRequestedOrder() throws Exception {
        Order first = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));
        Order second = service.createOrder(new OrderRequest("CUST-42", "Magic Mouse", 2));

        service.deleteOrder(first.getId());

        List<Order> remaining = service.getOrdersByCustomer("CUST-42");
        assertEquals(1, remaining.size());
        assertSame(second, remaining.get(0));
    }

    @Test
    void shouldNotReuseIdsAfterDelete() throws Exception {
        Order first = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));
        Order second = service.createOrder(new OrderRequest("CUST-42", "Magic Mouse", 2));

        service.deleteOrder(first.getId());
        Order third = service.createOrder(new OrderRequest("CUST-7", "iPad", 1));

        assertEquals("ORD-3", third.getId());
        assertNotEquals(second.getId(), third.getId());
    }

    @Test
    void shouldSkipIdsAlreadyTakenBySampleOrders() throws Exception {
        OrderService withExisting = new OrderService(List.of(new Order("ORD-2", "CUST-1", "Pen", 1)));

        Order created = withExisting.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        assertEquals("ORD-3", created.getId());
    }

    @Test
    void shouldRejectNullOrderId() {
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrder(null));

        assertEquals(ErrorType.INVALID_ORDER, ex.getType());
    }

    @Test
    void shouldRejectBlankOrderId() {
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrder("   "));

        assertEquals(ErrorType.INVALID_ORDER, ex.getType());
    }

    // getOrdersByCustomer

    @Test
    void shouldFindOrdersByCustomer() throws Exception {
        service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));
        service.createOrder(new OrderRequest("CUST-7", "iPad", 1));
        service.createOrder(new OrderRequest("CUST-42", "Magic Mouse", 2));

        List<Order> result = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoOrders() throws Exception {
        service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        List<Order> result = service.getOrdersByCustomer("CUST-100");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNullCustomerIdInSearch() {
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrdersByCustomer(null));

        assertEquals(ErrorType.INVALID_CUSTOMER, ex.getType());
    }

    @Test
    void shouldRejectBlankCustomerIdInSearch() {
        OrderException ex = assertThrows(OrderException.class, () -> service.getOrdersByCustomer(""));

        assertEquals(ErrorType.INVALID_CUSTOMER, ex.getType());
    }

    // invalid OrderRequest input

    @Test
    void shouldRejectNullCustomerId() {
        OrderException ex = assertThrows(OrderException.class, () -> new OrderRequest(null, "MacBook Air", 1));

        assertEquals(ErrorType.INVALID_CUSTOMER, ex.getType());
    }

    @Test
    void shouldRejectBlankProduct() {
        OrderException ex = assertThrows(OrderException.class, () -> new OrderRequest("CUST-42", " ", 1));

        assertEquals(ErrorType.INVALID_PRODUCT, ex.getType());
    }

    @Test
    void shouldRejectZeroQuantity() {
        OrderException ex = assertThrows(OrderException.class, () -> new OrderRequest("CUST-42", "MacBook Air", 0));

        assertEquals(ErrorType.INVALID_QUANTITY, ex.getType());
    }

    @Test
    void shouldRejectNegativeQuantity() {
        OrderException ex = assertThrows(OrderException.class, () -> new OrderRequest("CUST-42", "MacBook Air", -3));

        assertEquals(ErrorType.INVALID_QUANTITY, ex.getType());
    }

    @Test
    void shouldRejectRequestBuiltWithSettersMissingCustomer() {
        OrderRequest request = new OrderRequest();
        request.setProduct("MacBook Air");
        request.setQuantity(1);

        OrderException ex = assertThrows(OrderException.class, () -> service.createOrder(request));

        assertEquals(ErrorType.INVALID_CUSTOMER, ex.getType());
    }

}
