package com.example.secretencoder.dto;

import jakarta.validation.constraints.NotBlank;

public class TextEncodeRequest {

    private String coverText;

    @NotBlank(message = "Secret message cannot be empty")
    private String secretMessage;

    @NotBlank(message = "Password is required for encryption")
    private String password;

    public TextEncodeRequest() {
    }

    public TextEncodeRequest(String coverText, String secretMessage, String password) {
        this.coverText = coverText;
        this.secretMessage = secretMessage;
        this.password = password;
    }

    public String getCoverText() {
        return coverText;
    }

    public void setCoverText(String coverText) {
        this.coverText = coverText;
    }

    public String getSecretMessage() {
        return secretMessage;
    }

    public void setSecretMessage(String secretMessage) {
        this.secretMessage = secretMessage;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
