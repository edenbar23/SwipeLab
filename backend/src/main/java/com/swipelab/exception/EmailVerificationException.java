package com.swipelab.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailVerificationException extends RuntimeException {

    public EmailVerificationException(String message) {
        super(message);
    }

    public EmailVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}