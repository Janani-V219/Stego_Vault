package com.example.secretencoder.exception;

public class CapacityExceededException extends SteganographyException {
    private final long maxCapacityBytes;
    private final long requiredBytes;

    public CapacityExceededException(String message, long maxCapacityBytes, long requiredBytes) {
        super(message);
        this.maxCapacityBytes = maxCapacityBytes;
        this.requiredBytes = requiredBytes;
    }

    public long getMaxCapacityBytes() {
        return maxCapacityBytes;
    }

    public long getRequiredBytes() {
        return requiredBytes;
    }
}
