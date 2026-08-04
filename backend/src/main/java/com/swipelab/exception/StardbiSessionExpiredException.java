package com.swipelab.exception;

public class StardbiSessionExpiredException extends RuntimeException {
    public StardbiSessionExpiredException(String message) {
        super(message);
    }
}
