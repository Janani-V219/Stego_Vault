package com.example.secretencoder.exception;

public class DecryptionException extends SteganographyException {
    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
