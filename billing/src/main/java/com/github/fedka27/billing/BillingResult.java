package com.github.fedka27.billing;

public class BillingResult {
    private String message;
    private boolean success;
    public BillingResult () {/*do nothing*/}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
