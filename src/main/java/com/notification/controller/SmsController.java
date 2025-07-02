package com.notification.controller;

import com.notification.config.ApplicationConfig;
import com.notification.dto.NotificationDtos.ApiResponse;
import com.notification.dto.NotificationDtos.SmsRequestDto;
import com.notification.dto.NotificationDtos.SmsResponseDto;
import com.notification.model.SmsRequest;
import com.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class SmsController {

    private final SmsService smsService;
    
    // Health endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("message", "Service is running");
        healthStatus.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(healthStatus);
    }

    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<SmsResponseDto>> sendSms(@Valid @RequestBody SmsRequestDto requestDto) {
        log.info("Received SMS send request for phone number: {}", requestDto.getPhoneNumber());
        
        try {
            SmsResponseDto response = smsService.sendSms(requestDto);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error sending SMS for phone number: {}", requestDto.getPhoneNumber(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @GetMapping("/sms/{requestId}")
    public ResponseEntity<ApiResponse<SmsRequest>> getSmsRequest(@PathVariable String requestId) {
        log.info("Received request to get SMS details for request ID: {}", requestId);
        
        try {
            SmsRequest smsRequest = smsService.getSmsRequest(requestId);
            return ResponseEntity.ok(ApiResponse.success(smsRequest));
        } catch (RuntimeException e) {
            log.error("SMS request not found for ID: {}", requestId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ApplicationConfig.REQUEST_NOT_FOUND, "request_id not found"));
        } catch (Exception e) {
            log.error("Error retrieving SMS request for ID: {}", requestId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @GetMapping("/sms/id/{id}")
    public ResponseEntity<ApiResponse<SmsRequest>> getSmsRequestById(@PathVariable Long id) {
        log.info("Received request to get SMS details for database ID: {}", id);
        
        try {
            SmsRequest smsRequest = smsService.getSmsRequestById(id);
            return ResponseEntity.ok(ApiResponse.success(smsRequest));
        } catch (RuntimeException e) {
            log.error("SMS request not found for database ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ApplicationConfig.REQUEST_NOT_FOUND, "SMS request not found with ID: " + id));
        } catch (Exception e) {
            log.error("Error retrieving SMS request for database ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @DeleteMapping("/sms/id/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSmsRequest(@PathVariable Long id) {
        log.info("Received request to delete SMS request with database ID: {}", id);
        try {
            smsService.deleteSmsRequestById(id);
            return ResponseEntity.ok(ApiResponse.success("SMS request of databaseID " + id + " deleted successfully"));
        } catch (RuntimeException e) {
            log.error("SMS request not found for database ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ApplicationConfig.REQUEST_NOT_FOUND, "SMS request not found with ID: " + id));
        } catch (Exception e) {
            log.error("Error deleting SMS request for database ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }
} 