package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApplicationException{
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
