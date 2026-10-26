package co.edu.escuelaing;

public class Exceptions extends Exception {

    public enum InnerExceptions {
        ORDER_NOT_FOUND,
        INVALID_ORDER,
        INVALID_QUANTITY,
        INVALID_CUSTOMER,
        INVALID_PRODUCT
    }

    private final InnerExceptions type;

    private final String detail;

    public Exceptions(InnerExceptions type, String message){
        super(type + ": " + message);
        this.type = type;
        this.detail = message;
    }

    public InnerExceptions getExceptions(){
        return this.type;
    }

    public String getDetail(){
        return this.detail;
    }

}
