package com.agent.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {
    private int code;
    private String message;
    private List<FieldError> details;
    private LocalDateTime timestamp;

    public ErrorResponse(int code, String message, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    // Getter
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldError> getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // 内部类：单个字段错误
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}