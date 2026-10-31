package co.com.ai_finance.model.common.enums;

public enum ErrorMessages {
    ERROR_MESSSAGE_EMAIL_ALREADY_EXISTS("Already exists a user wit that email");

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
