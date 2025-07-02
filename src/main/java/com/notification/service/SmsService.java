package com.notification.service;

import com.notification.config.ApplicationConfig;
import com.notification.dto.NotificationDtos.SmsRequestDto;
import com.notification.dto.NotificationDtos.SmsResponseDto;
import com.notification.model.SmsRequest;
import com.notification.repository.SmsRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final SmsRequestRepository smsRequestRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BlacklistService blacklistService;

    @Transactional
    public SmsResponseDto sendSms(SmsRequestDto requestDto) {
        log.info("Processing SMS request for phone number: {}", requestDto.getPhoneNumber());
        
        // Check if phone number is blacklisted
        if (blacklistService.isBlacklisted(requestDto.getPhoneNumber())) {
            log.warn("Phone number {} is blacklisted", requestDto.getPhoneNumber());
            throw new RuntimeException("Phone number is blacklisted");
        }

        // Generate correlation ID
        String correlationId = generateCorrelationId();
        
        // Create SMS request entity
        SmsRequest smsRequest = SmsRequest.builder()
                .phoneNumber(requestDto.getPhoneNumber())
                .message(requestDto.getMessage())
                .status(SmsRequest.SmsStatus.PENDING)
                .correlationId(correlationId)
                .build();

        // Save to database
        SmsRequest savedRequest = smsRequestRepository.save(smsRequest);
        log.info("SMS request saved with ID: {}", savedRequest.getId());

        // Publish to Kafka topic
        kafkaTemplate.send(ApplicationConfig.SMS_SEND_TOPIC, correlationId);
        log.info("SMS request published to Kafka topic: {}", correlationId);

        return SmsResponseDto.builder()
                .databaseId(savedRequest.getId())
                .requestId(correlationId)
                .comments(ApplicationConfig.SMS_SENT_SUCCESS)
                .build();
    }

    public SmsRequest getSmsRequest(String requestId) {
        log.info("Fetching SMS request with correlation ID: {}", requestId);
        return smsRequestRepository.findByCorrelationId(requestId)
                .orElseThrow(() -> new RuntimeException("SMS request not found"));
    }

    public SmsRequest getSmsRequestById(Long id) {
        log.info("Fetching SMS request with database ID: {}", id);
        return smsRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SMS request not found with ID: " + id));
    }

    public void deleteSmsRequestById(Long id) {
        if (!smsRequestRepository.existsById(id)) {
            throw new RuntimeException("SMS request not found with ID: " + id);
        }
        smsRequestRepository.deleteById(id);
        log.info("Deleted SMS request with ID: {}", id);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
} 