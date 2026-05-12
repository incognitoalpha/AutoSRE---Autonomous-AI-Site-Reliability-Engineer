package com.autosre.gateway.dto;

import java.time.Instant;

/**
 * Standard error response format for all API error responses.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 *
 * @param error     human-readable error message
 * @param code      machine-readable error code
 * @param traceId   correlation ID for tracing
 * @param timestamp when the error occurred
 */
public record ErrorResponse(
        String error,
        String code,
        String traceId,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String code) {
        return new ErrorResponse(error, code, java.util.UUID.randomUUID().toString(), Instant.now());
    }
}