package com.autosre.common.exception;

/**
 * Base exception for all AutoSRE application-level exceptions.
 *
 * <p>Bounded context: {@code autosre-common}</p>
 */
public class AutoSreException extends RuntimeException {

    private final String errorCode;

    public AutoSreException(String message) {
        super(message);
        this.errorCode = "AUTOSRE_ERROR";
    }

    public AutoSreException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AUTOSRE_ERROR";
    }

    public AutoSreException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AutoSreException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}