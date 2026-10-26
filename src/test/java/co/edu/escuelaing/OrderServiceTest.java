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

import co.edu.escuelaing.Exceptions.InnerExceptions;

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
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrder("ORD-999"));

        assertEquals(InnerExceptions.ORDER_NOT_FOUND, ex.getExceptions());
    }

    @Test
    void shouldPrefixErrorMessageWithErrorCode() {
        // API Gateway maps errors to HTTP status codes by matching this prefix
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrder("ORD-999"));

        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", ex.getMessage());
    }

    // deleteOrder

    @Test
    void shouldDeleteExistingOrder() throws Exception {
        Order created = service.createOrder(new OrderRequest("CUST-42", "MacBook Air", 1));

        service.deleteOrder(created.getId());

        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrder(created.getId()));
        assertEquals(InnerExceptions.ORDER_NOT_FOUND, ex.getExceptions());
    }

    @Test
    void shouldFailWhenDeletingNonexistentOrder() {
        Exceptions ex = assertThrows(Exceptions.class, () -> service.deleteOrder("ORD-999"));

        assertEquals(InnerExceptions.ORDER_NOT_FOUND, ex.getExceptions());
    }

    @Test
    void shouldRejectBlankIdWhenDeleting() {
        Exceptions ex = assertThrows(Exceptions.class, () -> service.deleteOrder(" "));

        assertEquals(InnerExceptions.INVALID_ORDER, ex.getExceptions());
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
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrder(null));

        assertEquals(InnerExceptions.INVALID_ORDER, ex.getExceptions());
    }

    @Test
    void shouldRejectBlankOrderId() {
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrder("   "));

        assertEquals(InnerExceptions.INVALID_ORDER, ex.getExceptions());
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
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrdersByCustomer(null));

        assertEquals(InnerExceptions.INVALID_CUSTOMER, ex.getExceptions());
    }

    @Test
    void shouldRejectBlankCustomerIdInSearch() {
        Exceptions ex = assertThrows(Exceptions.class, () -> service.getOrdersByCustomer(""));

        assertEquals(InnerExceptions.INVALID_CUSTOMER, ex.getExceptions());
    }

    // invalid OrderRequest input

    @Test
    void shouldRejectNullCustomerId() {
        Exceptions ex = assertThrows(Exceptions.class, () -> new OrderRequest(null, "MacBook Air", 1));

        assertEquals(InnerExceptions.INVALID_CUSTOMER, ex.getExceptions());
    }

    @Test
    void shouldRejectBlankProduct() {
        Exceptions ex = assertThrows(Exceptions.class, () -> new OrderRequest("CUST-42", " ", 1));

        assertEquals(InnerExceptions.INVALID_PRODUCT, ex.getExceptions());
    }

    @Test
    void shouldRejectZeroQuantity() {
        Exceptions ex = assertThrows(Exceptions.class, () -> new OrderRequest("CUST-42", "MacBook Air", 0));

        assertEquals(InnerExceptions.INVALID_QUANTITY, ex.getExceptions());
    }

    @Test
    void shouldRejectNegativeQuantity() {
        Exceptions ex = assertThrows(Exceptions.class, () -> new OrderRequest("CUST-42", "MacBook Air", -3));

        assertEquals(InnerExceptions.INVALID_QUANTITY, ex.getExceptions());
    }

    @Test
    void shouldRejectRequestBuiltWithSettersMissingCustomer() {
        OrderRequest request = new OrderRequest();
        request.setProduct("MacBook Air");
        request.setQuantity(1);

        Exceptions ex = assertThrows(Exceptions.class, () -> service.createOrder(request));

        assertEquals(InnerExceptions.INVALID_CUSTOMER, ex.getExceptions());
    }

}
