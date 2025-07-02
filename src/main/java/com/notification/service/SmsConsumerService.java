package com.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.config.ApplicationConfig;
import com.notification.dto.NotificationDtos.SmsApiResponse;
import com.notification.model.SmsRequest;
import com.notification.repository.SmsRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsConsumerService {

    private final SmsRequestRepository smsRequestRepository;
    private final SmsApiService smsApiService;
    private final BlacklistService blacklistService;
    private final ElasticsearchService elasticsearchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = ApplicationConfig.SMS_SEND_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void processSmsRequest(String correlationId) {
        log.info("Processing SMS request with correlation ID: {}", correlationId);
        
        try {
            // Get SMS request from database
            SmsRequest smsRequest = smsRequestRepository.findByCorrelationId(correlationId)
                    .orElseThrow(() -> new RuntimeException("SMS request not found: " + correlationId));

            // Update status to processing
            smsRequest.setStatus(SmsRequest.SmsStatus.PROCESSING);
            smsRequestRepository.save(smsRequest);

            // Check if phone number is blacklisted
            if (blacklistService.isBlacklisted(smsRequest.getPhoneNumber())) {
                log.warn("Phone number {} is blacklisted, marking as blacklisted", smsRequest.getPhoneNumber());
                smsRequest.setStatus(SmsRequest.SmsStatus.BLACKLISTED);
                smsRequest.setFailureCode(ApplicationConfig.PHONE_NUMBER_BLACKLISTED);
                smsRequest.setFailureComments("Phone number is blacklisted");
                smsRequestRepository.save(smsRequest);
                return;
            }

            // Call third-party(instead of actual API, I'm using a mocked response) SMS API
            SmsApiResponse apiResponse = smsApiService.sendSms(smsRequest);
            
            // Update SMS request with response
            if (apiResponse.isSuccess()) {
                smsRequest.setStatus(SmsRequest.SmsStatus.SENT);
                smsRequest.setMessageId(apiResponse.getMessageId());
                log.info("SMS sent successfully for correlation ID: {}", correlationId);
            } else {
                smsRequest.setStatus(SmsRequest.SmsStatus.FAILED);
                smsRequest.setFailureCode(apiResponse.getErrorCode());
                smsRequest.setFailureComments(apiResponse.getErrorMessage());
                log.error("SMS sending failed for correlation ID: {}, error: {}", correlationId, apiResponse.getErrorMessage());
            }

            smsRequestRepository.save(smsRequest);

            // Index in Elasticsearch
            elasticsearchService.indexSmsRequest(smsRequest);
            log.info("SMS request indexed in Elasticsearch for correlation ID: {}", correlationId);

        } catch (Exception e) {
            log.error("Error processing SMS request with correlation ID: {}", correlationId, e);
            
            // Update status to failed
            try {
                SmsRequest smsRequest = smsRequestRepository.findByCorrelationId(correlationId).orElse(null);
                if (smsRequest != null) {
                    smsRequest.setStatus(SmsRequest.SmsStatus.FAILED);
                    smsRequest.setFailureCode("PROCESSING_ERROR");
                    smsRequest.setFailureComments(e.getMessage());
                    smsRequestRepository.save(smsRequest);
                }
            } catch (Exception updateException) {
                log.error("Error updating SMS request status for correlation ID: {}", correlationId, updateException);
            }
        }
    }
} 