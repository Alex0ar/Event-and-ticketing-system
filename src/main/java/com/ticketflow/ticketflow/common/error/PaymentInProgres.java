package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class PaymentInProgres extends ApplicationException {
    public PaymentInProgres(String message) {
        super(HttpStatus.CONFLICT, "PAYMENT_IN_PROGRESS", message);
    }
}
