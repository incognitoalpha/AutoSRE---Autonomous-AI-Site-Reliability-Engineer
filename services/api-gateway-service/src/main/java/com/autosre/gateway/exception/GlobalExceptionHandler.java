package com.autosre.gateway.exception;

import com.autosre.common.exception.AutoSreException;
import com.autosre.gateway.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

/**
 * Global exception handler that maps application exceptions to structured error responses.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AutoSreException.class)
    public ResponseEntity<ErrorResponse> handleAutoSreException(AutoSreException ex, ServerWebExchange exchange) {
        String traceId = extractTraceId(exchange);
        LOG.error("AutoSreException: code={}, message={}, traceId={}", ex.getErrorCode(), ex.getMessage(), traceId);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getErrorCode(), traceId, java.time.Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, ServerWebExchange exchange) {
        String traceId = extractTraceId(exchange);
        LOG.warn("IllegalArgumentException: message={}, traceId={}", ex.getMessage(), traceId);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "INVALID_ARGUMENT", traceId, java.time.Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String traceId = extractTraceId(exchange);
        LOG.error("Unhandled exception: traceId={}", traceId, ex);
        ErrorResponse response = new ErrorResponse("An unexpected error occurred", "INTERNAL_ERROR", traceId, java.time.Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String extractTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}