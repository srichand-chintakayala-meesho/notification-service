package com.notification.repository;

import com.notification.model.SmsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsRequestRepository extends JpaRepository<SmsRequest, Long> {

    Optional<SmsRequest> findByCorrelationId(String correlationId);

    @Query("SELECT s FROM SmsRequest s WHERE s.phoneNumber = :phoneNumber AND s.createdAt BETWEEN :startTime AND :endTime ORDER BY s.createdAt DESC")
    List<SmsRequest> findByPhoneNumberAndCreatedAtBetween(
            @Param("phoneNumber") String phoneNumber,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT s FROM SmsRequest s WHERE s.message LIKE %:text% ORDER BY s.createdAt DESC")
    List<SmsRequest> findByMessageContaining(@Param("text") String text);

    void deleteById(Long id);
} 