package com.notification.controller;

import com.notification.config.ApplicationConfig;
import com.notification.dto.NotificationDtos.ApiResponse;
import com.notification.dto.NotificationDtos.BlacklistRequestDto;
import com.notification.model.SmsRequest;
import com.notification.service.BlacklistService;
import com.notification.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Consolidated controller for Blacklist and Search operations
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class NotificationController {

    private final BlacklistService blacklistService;
    private final ElasticsearchService elasticsearchService;

    // Blacklist endpoints
    @PostMapping("/blacklist")
    public ResponseEntity<ApiResponse<String>> addToBlacklist(@Valid @RequestBody BlacklistRequestDto requestDto) {
        log.info("Received request to add phone numbers to blacklist: {}", requestDto.getPhoneNumbers());
        
        try {
            blacklistService.addToBlacklist(requestDto.getPhoneNumbers());
            return ResponseEntity.ok(ApiResponse.success(ApplicationConfig.BLACKLIST_SUCCESS));
        } catch (Exception e) {
            log.error("Error adding phone numbers to blacklist: {}", requestDto.getPhoneNumbers(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @DeleteMapping("/blacklist")
    public ResponseEntity<ApiResponse<String>> removeFromBlacklist(@Valid @RequestBody BlacklistRequestDto requestDto) {
        log.info("Received request to remove phone numbers from blacklist: {}", requestDto.getPhoneNumbers());
        
        try {
            blacklistService.removeFromBlacklist(requestDto.getPhoneNumbers());
            return ResponseEntity.ok(ApiResponse.success(ApplicationConfig.REMOVE_BLACKLIST_SUCCESS));
        } catch (Exception e) {
            log.error("Error removing phone numbers from blacklist: {}", requestDto.getPhoneNumbers(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @GetMapping("/blacklist")
    public ResponseEntity<ApiResponse<Set<String>>> getBlacklistedNumbers() {
        log.info("Received request to get all blacklisted phone numbers");
        
        try {
            Set<String> blacklistedNumbers = blacklistService.getBlacklistedNumbers();
            return ResponseEntity.ok(ApiResponse.success(blacklistedNumbers));
        } catch (Exception e) {
            log.error("Error retrieving blacklisted phone numbers", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    // Search endpoints
    @GetMapping("/search/sms/phone")
    public ResponseEntity<ApiResponse<Page<SmsRequest>>> searchSmsByPhoneNumberAndTimeRange(
            @RequestParam @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in international format") String phoneNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Searching SMS for phone number: {} between {} and {}", phoneNumber, startTime, endTime);
        // log.info("Searching SMS for phone number: {} between {} and {}", phoneNumber);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SmsRequest> result = elasticsearchService.searchSmsByPhoneNumberAndTimeRange(
                    phoneNumber, startTime, endTime, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error searching SMS by phone number and time range: {}", phoneNumber, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }

    @GetMapping("/search/sms/text")
    public ResponseEntity<ApiResponse<Page<SmsRequest>>> searchSmsByText(
            @RequestParam @NotBlank(message = "Search text is mandatory") String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Searching SMS containing text: {}", text);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SmsRequest> result = elasticsearchService.searchSmsByText(text, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error searching SMS by text: {}", text, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApplicationConfig.INVALID_REQUEST, e.getMessage()));
        }
    }
} 