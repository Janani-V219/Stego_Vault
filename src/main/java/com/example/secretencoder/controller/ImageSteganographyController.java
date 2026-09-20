package com.example.secretencoder.controller;

import com.example.secretencoder.dto.ApiResponse;
import com.example.secretencoder.dto.CapacityResponse;
import com.example.secretencoder.entity.OperationStatus;
import com.example.secretencoder.entity.OperationType;
import com.example.secretencoder.security.UserPrincipal;
import com.example.secretencoder.service.HistoryService;
import com.example.secretencoder.steganography.ImageSteganographyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/steganography/image")
public class ImageSteganographyController {

    private final ImageSteganographyService imageSteganographyService;
    private final HistoryService historyService;

    public ImageSteganographyController(ImageSteganographyService imageSteganographyService,
                                        HistoryService historyService) {
        this.imageSteganographyService = imageSteganographyService;
        this.historyService = historyService;
    }

    @PostMapping("/capacity")
    public ResponseEntity<ApiResponse<CapacityResponse>> checkCapacity(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "messageLength", required = false, defaultValue = "0") int messageLength) {

        BufferedImage img = imageSteganographyService.readAndNormalizeImage(image);
        int width = img.getWidth();
        int height = img.getHeight();

        long maxCapacityBytes = imageSteganographyService.calculateMaxCapacityBytes(width, height);
        long requiredBytes = imageSteganographyService.calculateRequiredBytes(messageLength);
        boolean canFit = requiredBytes <= maxCapacityBytes;

        String statusMessage = canFit
                ? "✓ Image can store this message safely."
                : "❌ Message is too large for this image. Please choose a larger image.";

        CapacityResponse capacity = new CapacityResponse(
                width, height, maxCapacityBytes, requiredBytes, canFit, statusMessage
        );

        return ResponseEntity.ok(ApiResponse.success("Image capacity calculated", capacity));
    }

    @PostMapping(value = "/encode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> encodeMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("message") String message,
            @RequestParam("password") String password,
            @AuthenticationPrincipal UserPrincipal principal) {

        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "secret-image.png";
        }

        Long userId = (principal != null) ? principal.getId() : null;

        try {
            byte[] encodedImageBytes = imageSteganographyService.encodeMessage(image, message, password);

            historyService.recordHistory(
                    userId,
                    OperationType.IMAGE_ENCODE,
                    originalFilename,
                    "image/png",
                    OperationStatus.COMPLETED
            );

            String downloadFilename = "encoded-" + originalFilename;
            if (!downloadFilename.toLowerCase().endsWith(".png")) {
                downloadFilename += ".png";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFilename + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(encodedImageBytes);
        } catch (Exception e) {
            historyService.recordHistory(
                    userId,
                    OperationType.IMAGE_ENCODE,
                    originalFilename,
                    "image/png",
                    OperationStatus.FAILED
            );
            throw e;
        }
    }

    @PostMapping("/decode")
    public ResponseEntity<ApiResponse<Map<String, String>>> decodeMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("password") String password,
            @AuthenticationPrincipal UserPrincipal principal) {

        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "encoded-image.png";
        }

        Long userId = (principal != null) ? principal.getId() : null;

        try {
            String secretMessage = imageSteganographyService.decodeMessage(image, password);

            historyService.recordHistory(
                    userId,
                    OperationType.IMAGE_DECODE,
                    originalFilename,
                    "image/png",
                    OperationStatus.COMPLETED
            );

            Map<String, String> result = new HashMap<>();
            result.put("secretMessage", secretMessage);
            result.put("fileName", originalFilename);

            return ResponseEntity.ok(ApiResponse.success("Message extracted successfully", result));
        } catch (Exception e) {
            historyService.recordHistory(
                    userId,
                    OperationType.IMAGE_DECODE,
                    originalFilename,
                    "image/png",
                    OperationStatus.FAILED
            );
            throw e;
        }
    }
}
