package com.example.secretencoder.dto;

import jakarta.validation.constraints.NotBlank;

public class TextDecodeRequest {

    @NotBlank(message = "Encoded text cannot be empty")
    private String encodedText;

    @NotBlank(message = "Password is required for decryption")
    private String password;

    public TextDecodeRequest() {
    }

    public TextDecodeRequest(String encodedText, String password) {
        this.encodedText = encodedText;
        this.password = password;
    }

    public String getEncodedText() {
        return encodedText;
    }

    public void setEncodedText(String encodedText) {
        this.encodedText = encodedText;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
