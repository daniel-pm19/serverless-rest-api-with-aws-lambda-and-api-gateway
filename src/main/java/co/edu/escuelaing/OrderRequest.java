package co.edu.escuelaing;

import co.edu.escuelaing.OrderException.ErrorType;

public class OrderRequest {

    private String customerId;

    private String product;

    private int quantity;

    public OrderRequest(String customerId, String product, int quantity) throws OrderException {
        this.customerId = validateCustomerId(customerId);
        this.product = validateProduct(product);
        this.quantity = validateQuantity(quantity);
    }

    public OrderRequest() {}

    public String getCustomerId() {
        return this.customerId;
    }

    public String getProduct() {
        return this.product;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setCustomerId(String newCustomerId) {
        this.customerId = newCustomerId;
    }

    public void setProduct(String newProduct) {
        this.product = newProduct;
    }

    public void setQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }

    public void validate() throws OrderException {
        validateCustomerId(customerId);
        validateProduct(product);
        validateQuantity(quantity);
    }   

    private String validateCustomerId(String id) throws OrderException {
        if (id == null || id.isBlank()) {
            throw new OrderException(ErrorType.INVALID_CUSTOMER, "Customer id cannot be null or blank");
        }

        return id;
    }

    private int validateQuantity(int quantity) throws OrderException {
        if (quantity <= 0) {
            throw new OrderException(ErrorType.INVALID_QUANTITY, "Quantity must be > 0");
        }

        return quantity;
    }

    private String validateProduct(String product) throws OrderException {
        if (product == null || product.isBlank()) {
            throw new OrderException(ErrorType.INVALID_PRODUCT, "Product cannot be null or blank");
        }

        return product;
    }
}
