package com.example.secretencoder.controller;

import com.example.secretencoder.dto.ApiResponse;
import com.example.secretencoder.dto.HistoryDto;
import com.example.secretencoder.security.UserPrincipal;
import com.example.secretencoder.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoryDto>>> getUserHistory(@AuthenticationPrincipal UserPrincipal principal) {
        List<HistoryDto> historyList = historyService.getUserHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Encoding history retrieved successfully", historyList));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistoryItem(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        historyService.deleteHistoryItem(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("History record deleted successfully"));
    }
}
