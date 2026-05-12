package com.autosre.common.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for time-related operations.
 *
 * <p>Bounded context: {@code autosre-common}</p>
 */
public final class TimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private TimeUtils() {
        // Utility class, no instantiation
    }

    /**
     * Formats an Instant to an ISO-8601 string.
     *
     * @param instant the instant to format
     * @return formatted string in ISO-8601 format
     */
    public static String formatIso(Instant instant) {
        if (instant == null) {
            return "";
        }
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Parses an ISO-8601 string to an Instant.
     *
     * @param isoString the ISO-8601 formatted string
     * @return the parsed Instant
     */
    public static Instant parseIso(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        return Instant.parse(isoString);
    }

    /**
     * Calculates the duration between two instants in milliseconds.
     *
     * @param start the start instant
     * @param end the end instant
     * @return duration in milliseconds
     */
    public static long durationMs(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toMillis();
    }

    /**
     * Returns the current UTC time as a ZonedDateTime.
     *
     * @return current time in UTC
     */
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC);
    }

    /**
     * Formats a ZonedDateTime to a human-readable string.
     *
     * @param dateTime the date-time to format
     * @return formatted string
     */
    public static String formatReadable(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(dateTime);
    }
}