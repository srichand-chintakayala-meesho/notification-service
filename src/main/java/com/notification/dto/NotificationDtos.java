package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * Consolidated DTOs for the Notification Service
 */
public class NotificationDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsRequestDto {
        @NotBlank(message = "phone_number is mandatory")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in international format")
        private String phoneNumber;

        @NotBlank(message = "message is mandatory")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsResponseDto {
        private String requestId;
        private Long databaseId;
        private String comments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlacklistRequestDto {
        @NotEmpty(message = "phone_numbers list cannot be empty")
        private List<@Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in international format") String> phoneNumbers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsApiResponse {
        private boolean success;
        private String messageId;
        private String errorCode;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private T data;
        private ErrorResponse error;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().data(data).build();
        }

        public static <T> ApiResponse<T> error(String code, String message) {
            return ApiResponse.<T>builder()
                    .error(ErrorResponse.builder().code(code).message(message).build())
                    .build();
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ErrorResponse {
            private String code;
            private String message;
        }
    }
} 