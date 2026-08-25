package com.ticketflow.ticketflow.common.error;

import org.springframework.http.HttpStatus;

public class ForbidenException extends ApplicationException{
    public ForbidenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
