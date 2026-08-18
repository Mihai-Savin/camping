package com.camp.reservations.exception;

public class DescriptionGenerationException extends RuntimeException {
    public DescriptionGenerationException(String message) {
        super(message);
    }

    public DescriptionGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
