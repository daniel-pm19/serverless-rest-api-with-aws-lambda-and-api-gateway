package co.edu.escuelaing;

public class Order {

    private String id;

    private String customerId;

    private String product;

    private int quantity;

    public Order(String id, String customerId, String product, int quantity) {
        this.id = id;
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getId() {
        return this.id;
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public String getProduct() {
        return this.product;
    }

    public int getQuantity() {
        return this.quantity;
    }

}