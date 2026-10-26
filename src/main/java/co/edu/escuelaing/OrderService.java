package co.edu.escuelaing;

import java.util.ArrayList;
import java.util.List;

import co.edu.escuelaing.Exceptions.InnerExceptions;

public class OrderService {

    private List<Order> orders;

    public OrderService() {
        this(List.of(
                new Order("ORD-42", "CUST-42", "MacBook Air", 1),
                new Order("ORD-43", "CUST-42", "Magic Mouse", 2),
                new Order("ORD-44", "CUST-7", "iPad", 1)
        ));
    }

    public OrderService(List<Order> initialOrders) {
        this.orders = new ArrayList<>(initialOrders);
    }

    public Order createOrder(OrderRequest request) throws Exceptions {
        if (request == null) {
            throw new Exceptions(InnerExceptions.INVALID_ORDER, "Request body cannot be null");
        }

        request.validate();

        Order newOrder = new Order(
                generateOrderId(),
                request.getCustomerId(),
                request.getProduct(),
                request.getQuantity());

        orders.add(newOrder);
        return newOrder;
    }

    public Order getOrder(String id) throws Exceptions {
        if (id == null || id.isBlank()) {
            throw new Exceptions(InnerExceptions.INVALID_ORDER, "Order id cannot be null or blank");
        }

        Order order = null;

        for (Order o : orders) {
            if (o.getId().equals(id)) {
                order = o;
            }
        }

        if (order == null) {
            throw new Exceptions(InnerExceptions.ORDER_NOT_FOUND, "Order with id " + id + " was not Found");
        }

        return order;

    }

    public List<Order> getOrdersByCustomer(String id) throws Exceptions {
        if (id == null || id.isBlank()) {
            throw new Exceptions(InnerExceptions.INVALID_CUSTOMER, "Customer id cannot be blank or null");
        }

        List<Order> customerOrders = new ArrayList<>();

        for (Order o : orders) {
            if (o.getCustomerId().equals(id)) {
                customerOrders.add(o);
            }
        }

        return customerOrders;
    }

    private String generateOrderId() {
        return "ORD-" + (orders.size() + 1);
    }
}
