package com.example.secretencoder.controller;

import com.example.secretencoder.dto.ApiResponse;
import com.example.secretencoder.dto.TextDecodeRequest;
import com.example.secretencoder.dto.TextDecodeResponse;
import com.example.secretencoder.dto.TextEncodeRequest;
import com.example.secretencoder.dto.TextEncodeResponse;
import com.example.secretencoder.entity.OperationStatus;
import com.example.secretencoder.entity.OperationType;
import com.example.secretencoder.security.UserPrincipal;
import com.example.secretencoder.service.HistoryService;
import com.example.secretencoder.steganography.TextSteganographyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/steganography/text")
public class TextSteganographyController {

    private final TextSteganographyService textSteganographyService;
    private final HistoryService historyService;

    public TextSteganographyController(TextSteganographyService textSteganographyService,
                                       HistoryService historyService) {
        this.textSteganographyService = textSteganographyService;
        this.historyService = historyService;
    }

    @PostMapping("/encode")
    public ResponseEntity<ApiResponse<TextEncodeResponse>> encodeText(
            @Valid @RequestBody TextEncodeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = (principal != null) ? principal.getId() : null;

        try {
            TextSteganographyService.TextEncodeResult result = textSteganographyService.encode(
                    request.getCoverText(),
                    request.getSecretMessage(),
                    request.getPassword()
            );

            historyService.recordHistory(
                    userId,
                    OperationType.TEXT_ENCODE,
                    "secret-text.txt",
                    "text/plain",
                    OperationStatus.COMPLETED
            );

            TextEncodeResponse response = new TextEncodeResponse(
                    result.encodedText(),
                    result.coverLength(),
                    result.hiddenPayloadBytes(),
                    "Secret message embedded invisibly into text using zero-width steganography."
            );

            return ResponseEntity.ok(ApiResponse.success("Text encoded successfully", response));
        } catch (Exception e) {
            historyService.recordHistory(
                    userId,
                    OperationType.TEXT_ENCODE,
                    "secret-text.txt",
                    "text/plain",
                    OperationStatus.FAILED
            );
            throw e;
        }
    }

    @PostMapping("/decode")
    public ResponseEntity<ApiResponse<TextDecodeResponse>> decodeText(
            @Valid @RequestBody TextDecodeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = (principal != null) ? principal.getId() : null;

        try {
            String secretMessage = textSteganographyService.decode(
                    request.getEncodedText(),
                    request.getPassword()
            );

            historyService.recordHistory(
                    userId,
                    OperationType.TEXT_DECODE,
                    "secret-text.txt",
                    "text/plain",
                    OperationStatus.COMPLETED
            );

            TextDecodeResponse response = new TextDecodeResponse(
                    secretMessage,
                    "Message successfully extracted and decrypted."
            );

            return ResponseEntity.ok(ApiResponse.success("Text decoded successfully", response));
        } catch (Exception e) {
            historyService.recordHistory(
                    userId,
                    OperationType.TEXT_DECODE,
                    "secret-text.txt",
                    "text/plain",
                    OperationStatus.FAILED
            );
            throw e;
        }
    }
}
