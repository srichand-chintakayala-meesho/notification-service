package com.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.config.ApplicationConfig;
import com.notification.dto.NotificationDtos.SmsApiResponse;
import com.notification.model.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sms.api.url}")
    private String apiUrl;

    @Value("${sms.api.key}")
    private String apiKey;

    @Value("${sms.api.timeout}")
    private int timeout;

    public SmsApiResponse sendSms(SmsRequest smsRequest) {
        log.info("Sending SMS via third-party API for correlation ID: {}", smsRequest.getCorrelationId());
        
        try {
            // Prepare request payload (for logging purposes)
            List<Map<String, Object>> requestPayload = createRequestPayload(smsRequest);
            log.debug("SMS API request payload: {}", objectMapper.writeValueAsString(requestPayload));
            
            // MOCKED RESPONSE
            log.info("🔧 MOCKED SMS API RESPONSE for correlation ID: {}", smsRequest.getCorrelationId());
            return SmsApiResponse.builder()
                    .success(true)
                    .messageId("MOCK_MSG_" + System.currentTimeMillis())
                    .build();
            
        } catch (Exception e) {
            log.error("SMS API error for correlation ID: {}", smsRequest.getCorrelationId(), e);
            return SmsApiResponse.builder()
                    .success(false)
                    .errorCode("API_ERROR")
                    .errorMessage("API Error: " + e.getMessage())
                    .build();
        }
    }

    private List<Map<String, Object>> createRequestPayload(SmsRequest smsRequest) {
        List<Map<String, Object>> payload = new ArrayList<>();
        
        Map<String, Object> request = new HashMap<>();
        request.put("deliverychannel", ApplicationConfig.SMS_API_DELIVERY_CHANNEL);
        
        Map<String, Object> channels = new HashMap<>();
        Map<String, Object> smsChannel = new HashMap<>();
        smsChannel.put("text", smsRequest.getMessage());
        channels.put(ApplicationConfig.SMS_API_CHANNEL_TYPE, smsChannel);
        request.put("channels", channels);
        
        Map<String, Object> destination = new HashMap<>();
        List<String> msisdn = new ArrayList<>();
        msisdn.add(smsRequest.getPhoneNumber());
        destination.put("msisdn", msisdn);
        destination.put("correlationid", smsRequest.getCorrelationId());
        
        List<Map<String, Object>> destinations = new ArrayList<>();
        destinations.add(destination);
        request.put("destination", destinations);
        
        payload.add(request);
        return payload;
    }

    private SmsApiResponse parseApiResponse(String responseBody) {
        try {
            JsonNode responseNode = objectMapper.readTree(responseBody);
            
            // Check if response indicates success (this may need adjustment based on actual API response format)
            if (responseNode.has("status") && "success".equalsIgnoreCase(responseNode.get("status").asText())) {
                String messageId = responseNode.has("message_id") ? responseNode.get("message_id").asText() : null;
                return SmsApiResponse.builder()
                        .success(true)
                        .messageId(messageId)
                        .build();
            } else {
                String errorCode = responseNode.has("error_code") ? responseNode.get("error_code").asText() : "UNKNOWN_ERROR";
                String errorMessage = responseNode.has("error_message") ? responseNode.get("error_message").asText() : "Unknown error";
                return SmsApiResponse.builder()
                        .success(false)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error parsing SMS API response: {}", responseBody, e);
            return SmsApiResponse.builder()
                    .success(false)
                    .errorCode("PARSE_ERROR")
                    .errorMessage("Error parsing API response: " + e.getMessage())
                    .build();
        }
    }
} 