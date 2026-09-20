package com.example.secretencoder.dto;

public class TextDecodeResponse {

    private String secretMessage;
    private String status;

    public TextDecodeResponse() {
    }

    public TextDecodeResponse(String secretMessage, String status) {
        this.secretMessage = secretMessage;
        this.status = status;
    }

    public String getSecretMessage() {
        return secretMessage;
    }

    public void setSecretMessage(String secretMessage) {
        this.secretMessage = secretMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
