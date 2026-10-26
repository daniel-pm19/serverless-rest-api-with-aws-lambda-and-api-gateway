package co.edu.escuelaing;

import co.edu.escuelaing.Exceptions.InnerExceptions;

public class OrderRequest {

    private String customerId;

    private String product;

    private int quantity;

    public OrderRequest(String customerId, String product, int quantity) throws Exceptions {
        this.customerId = validateCustomerId(customerId);
        this.product = validateProduct(product);
        this.quantity = validateQuantity(quantity);
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

    private String validateCustomerId(String id) throws Exceptions {
        if (id == null || id.isBlank()) {
            throw new Exceptions(InnerExceptions.INVALID_CUSTOMER, "Customer id cannot be null or blank");
        }

        return id;
    }

    private int validateQuantity(int quantity) throws Exceptions {
        if(quantity <= 0){
            throw new Exceptions(InnerExceptions.INVALID_QUANTITY, "Quantity must be > 0");
        }

        return quantity;
    }

    private String validateProduct(String product) throws Exceptions {
        if (product == null || product.isBlank()) {
            throw new Exceptions(InnerExceptions.INVALID_PRODUCT, "Product cannot be null or blank");
        }

        return product;
    }
}
