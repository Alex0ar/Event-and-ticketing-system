package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class FailedPaymentException extends ApplicationException {
    public FailedPaymentException(String message) {
        super(HttpStatus.CONFLICT, "PAYMENT_FAILED", message);
    }
}
