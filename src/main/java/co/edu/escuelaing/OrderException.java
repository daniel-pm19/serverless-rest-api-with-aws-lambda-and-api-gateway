package co.edu.escuelaing;

public class OrderException extends Exception {

    public enum ErrorType {
        ORDER_NOT_FOUND,
        INVALID_ORDER,
        INVALID_QUANTITY,
        INVALID_CUSTOMER,
        INVALID_PRODUCT
    }

    private final ErrorType type;

    private final String detail;

    public OrderException(ErrorType type, String message){
        super(type + ": " + message);
        this.type = type;
        this.detail = message;
    }

    public ErrorType getType(){
        return this.type;
    }

    public String getDetail(){
        return this.detail;
    }

}
