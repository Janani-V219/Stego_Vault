package com.example.secretencoder.dto;

public class TextEncodeResponse {

    private String encodedText;
    private int coverLength;
    private int hiddenPayloadBytes;
    private String message;

    public TextEncodeResponse() {
    }

    public TextEncodeResponse(String encodedText, int coverLength, int hiddenPayloadBytes, String message) {
        this.encodedText = encodedText;
        this.coverLength = coverLength;
        this.hiddenPayloadBytes = hiddenPayloadBytes;
        this.message = message;
    }

    public String getEncodedText() {
        return encodedText;
    }

    public void setEncodedText(String encodedText) {
        this.encodedText = encodedText;
    }

    public int getCoverLength() {
        return coverLength;
    }

    public void setCoverLength(int coverLength) {
        this.coverLength = coverLength;
    }

    public int getHiddenPayloadBytes() {
        return hiddenPayloadBytes;
    }

    public void setHiddenPayloadBytes(int hiddenPayloadBytes) {
        this.hiddenPayloadBytes = hiddenPayloadBytes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
