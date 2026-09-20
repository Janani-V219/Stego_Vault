package com.example.secretencoder.service;

import com.example.secretencoder.dto.HistoryDto;
import com.example.secretencoder.entity.EncodingHistory;
import com.example.secretencoder.entity.OperationStatus;
import com.example.secretencoder.entity.OperationType;
import com.example.secretencoder.entity.User;
import com.example.secretencoder.exception.SteganographyException;
import com.example.secretencoder.repository.EncodingHistoryRepository;
import com.example.secretencoder.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final EncodingHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryService(EncodingHistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordHistory(Long userId, OperationType type, String fileName, String fileType, OperationStatus status) {
        if (userId == null) {
            return; // Anonymous operation, skip recording
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            EncodingHistory history = new EncodingHistory(
                    type,
                    (fileName != null && !fileName.isEmpty()) ? fileName : "unnamed",
                    (fileType != null && !fileType.isEmpty()) ? fileType : "data",
                    status,
                    user
            );
            historyRepository.save(history);
        }
    }

    @Transactional(readOnly = true)
    public List<HistoryDto> getUserHistory(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(h -> new HistoryDto(
                        h.getId(),
                        h.getOperationType(),
                        h.getFileName(),
                        h.getFileType(),
                        h.getStatus(),
                        h.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteHistoryItem(Long historyId, Long userId) {
        EncodingHistory item = historyRepository.findById(historyId)
                .orElseThrow(() -> new SteganographyException("History record not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to delete this history record");
        }

        historyRepository.delete(item);
    }
}
