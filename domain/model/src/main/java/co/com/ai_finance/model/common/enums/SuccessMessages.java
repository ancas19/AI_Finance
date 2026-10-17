package co.com.ai_finance.model.common.enums;

public enum SuccessMessages {
    SUCCESS_MESSAGE_HEALTH("App is working");

    private final String message;


    SuccessMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
