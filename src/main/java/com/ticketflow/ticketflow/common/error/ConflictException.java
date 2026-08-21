package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException{
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
