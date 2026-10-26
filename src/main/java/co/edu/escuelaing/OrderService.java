package co.edu.escuelaing;

import java.util.ArrayList;
import java.util.List;

import co.edu.escuelaing.OrderException.ErrorType;

public class OrderService {

    private List<Order> orders;

    private int nextId;

    public OrderService() {
        this(List.of(
                new Order("ORD-42", "CUST-42", "MacBook Air", 1),
                new Order("ORD-43", "CUST-42", "Magic Mouse", 2),
                new Order("ORD-44", "CUST-7", "iPad", 1)
        ));
    }

    public OrderService(List<Order> initialOrders) {
        this.orders = new ArrayList<>(initialOrders);
        this.nextId = this.orders.size() + 1;
    }

    public Order createOrder(OrderRequest request) throws OrderException {
        if (request == null) {
            throw new OrderException(ErrorType.INVALID_ORDER, "Request body cannot be null");
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

    public Order getOrder(String id) throws OrderException {
        if (id == null || id.isBlank()) {
            throw new OrderException(ErrorType.INVALID_ORDER, "Order id cannot be null or blank");
        }

        Order order = findOrder(id);

        if (order == null) {
            throw new OrderException(ErrorType.ORDER_NOT_FOUND, "Order " + id + " does not exist");
        }

        return order;

    }

    public List<Order> getOrdersByCustomer(String id) throws OrderException {
        if (id == null || id.isBlank()) {
            throw new OrderException(ErrorType.INVALID_CUSTOMER, "Customer id cannot be blank or null");
        }

        List<Order> customerOrders = new ArrayList<>();

        for (Order o : orders) {
            if (o.getCustomerId().equals(id)) {
                customerOrders.add(o);
            }
        }

        return customerOrders;
    }

    public void deleteOrder(String id) throws OrderException {
        if (id == null || id.isBlank()) {
            throw new OrderException(ErrorType.INVALID_ORDER, "Order id cannot be null or blank");
        }

        Order order = findOrder(id);

        if (order == null) {
            throw new OrderException(ErrorType.ORDER_NOT_FOUND, "Order " + id + " does not exist");
        }

        orders.remove(order);
    }

    private Order findOrder(String id) {
        for (Order o : orders) {
            if (o.getId().equals(id)) {
                return o;
            }
        }
        return null;
    }

    // Ids are never reused: the counter only grows and skips ids already taken (e.g. the sample ORD-42)
    private String generateOrderId() {
        String id;
        do {
            id = "ORD-" + nextId++;
        } while (findOrder(id) != null);
        return id;
    }
}
