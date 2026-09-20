package com.example.secretencoder.controller;

import com.example.secretencoder.dto.AuthResponse;
import com.example.secretencoder.dto.RegisterRequest;
import com.example.secretencoder.entity.OperationStatus;
import com.example.secretencoder.entity.OperationType;
import com.example.secretencoder.entity.User;
import com.example.secretencoder.repository.UserRepository;
import com.example.secretencoder.service.AuthService;
import com.example.secretencoder.service.HistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should reject unauthenticated access to history")
    void testUnauthenticatedHistoryAccess() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow authenticated user to retrieve and delete their history")
    void testUserHistoryFlow() throws Exception {
        // Register user 1
        String email = "historyuser1_" + System.currentTimeMillis() + "@test.com";
        AuthResponse auth = authService.register(new RegisterRequest("User One", email, "Password123!", "Password123!"));
        String token = auth.getToken();

        // Record a history item
        historyService.recordHistory(auth.getUserId(), OperationType.IMAGE_ENCODE, "test.png", "image/png", OperationStatus.COMPLETED);

        // Fetch history
        mockMvc.perform(get("/api/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fileName").value("test.png"))
                .andExpect(jsonPath("$.data[0].operationType").value("IMAGE_ENCODE"));
    }

    @Test
    @DisplayName("Should forbid user from deleting another user's history item")
    void testCannotDeleteOtherUserHistory() throws Exception {
        // User A
        String emailA = "userA_" + System.currentTimeMillis() + "@test.com";
        AuthResponse authA = authService.register(new RegisterRequest("User A", emailA, "Password123!", "Password123!"));
        historyService.recordHistory(authA.getUserId(), OperationType.IMAGE_ENCODE, "userA.png", "image/png", OperationStatus.COMPLETED);
        Long historyItemId = historyService.getUserHistory(authA.getUserId()).get(0).getId();

        // User B
        String emailB = "userB_" + System.currentTimeMillis() + "@test.com";
        AuthResponse authB = authService.register(new RegisterRequest("User B", emailB, "Password123!", "Password123!"));

        // User B attempts to delete User A's history record
        mockMvc.perform(delete("/api/history/" + historyItemId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authB.getToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not authorized to delete this history record"));
    }
}
