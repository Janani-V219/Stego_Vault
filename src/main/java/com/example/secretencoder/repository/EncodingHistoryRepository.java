package com.example.secretencoder.repository;

import com.example.secretencoder.entity.EncodingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncodingHistoryRepository extends JpaRepository<EncodingHistory, Long> {
    List<EncodingHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<EncodingHistory> findByIdAndUserId(Long id, Long userId);
}
