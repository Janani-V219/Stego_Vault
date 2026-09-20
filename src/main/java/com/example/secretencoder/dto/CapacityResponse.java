package com.example.secretencoder.dto;

public class CapacityResponse {

    private int width;
    private int height;
    private long maxCapacityBytes;
    private double maxCapacityKb;
    private long requiredBytes;
    private double requiredKb;
    private double usagePercentage;
    private boolean canFit;
    private String statusMessage;

    public CapacityResponse() {
    }

    public CapacityResponse(int width, int height, long maxCapacityBytes, long requiredBytes, boolean canFit, String statusMessage) {
        this.width = width;
        this.height = height;
        this.maxCapacityBytes = maxCapacityBytes;
        this.maxCapacityKb = Math.round((maxCapacityBytes / 1024.0) * 100.0) / 100.0;
        this.requiredBytes = requiredBytes;
        this.requiredKb = Math.round((requiredBytes / 1024.0) * 100.0) / 100.0;
        this.usagePercentage = maxCapacityBytes > 0
                ? Math.min(100.0, Math.round(((double) requiredBytes / maxCapacityBytes * 100.0) * 10.0) / 10.0)
                : 0.0;
        this.canFit = canFit;
        this.statusMessage = statusMessage;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getMaxCapacityBytes() {
        return maxCapacityBytes;
    }

    public void setMaxCapacityBytes(long maxCapacityBytes) {
        this.maxCapacityBytes = maxCapacityBytes;
    }

    public double getMaxCapacityKb() {
        return maxCapacityKb;
    }

    public void setMaxCapacityKb(double maxCapacityKb) {
        this.maxCapacityKb = maxCapacityKb;
    }

    public long getRequiredBytes() {
        return requiredBytes;
    }

    public void setRequiredBytes(long requiredBytes) {
        this.requiredBytes = requiredBytes;
    }

    public double getRequiredKb() {
        return requiredKb;
    }

    public void setRequiredKb(double requiredKb) {
        this.requiredKb = requiredKb;
    }

    public double getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(double usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public boolean isCanFit() {
        return canFit;
    }

    public void setCanFit(boolean canFit) {
        this.canFit = canFit;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
