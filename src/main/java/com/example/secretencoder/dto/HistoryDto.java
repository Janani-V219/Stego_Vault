package com.example.secretencoder.dto;

import com.example.secretencoder.entity.OperationStatus;
import com.example.secretencoder.entity.OperationType;

import java.time.LocalDateTime;

public class HistoryDto {

    private Long id;
    private OperationType operationType;
    private String fileName;
    private String fileType;
    private OperationStatus status;
    private LocalDateTime createdAt;

    public HistoryDto() {
    }

    public HistoryDto(Long id, OperationType operationType, String fileName, String fileType, OperationStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.operationType = operationType;
        this.fileName = fileName;
        this.fileType = fileType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(OperationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
