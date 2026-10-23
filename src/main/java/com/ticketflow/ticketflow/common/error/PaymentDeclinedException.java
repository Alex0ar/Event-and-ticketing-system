package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class PaymentDeclinedException extends ApplicationException {
    public PaymentDeclinedException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED", message);
    }
}
