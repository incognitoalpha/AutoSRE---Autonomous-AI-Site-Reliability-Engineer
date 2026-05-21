package com.autosre.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeUtilsTest {

    @Test
    void testFormatIso_Success() {
        Instant instant = Instant.ofEpochMilli(1609459200000L); // 2021-01-01T00:00:00Z
        String iso = TimeUtils.formatIso(instant);
        assertEquals("2021-01-01T00:00:00Z", iso);
    }

    @Test
    void testFormatIso_Null() {
        assertEquals("", TimeUtils.formatIso(null));
    }

    @Test
    void testParseIso_Success() {
        String iso = "2021-01-01T00:00:00Z";
        Instant instant = TimeUtils.parseIso(iso);
        assertNotNull(instant);
        assertEquals(1609459200000L, instant.toEpochMilli());
    }

    @Test
    void testParseIso_NullOrBlank() {
        assertNull(TimeUtils.parseIso(null));
        assertNull(TimeUtils.parseIso(""));
        assertNull(TimeUtils.parseIso("   "));
    }

    @Test
    void testParseIso_Invalid() {
        assertThrows(DateTimeParseException.class, () -> TimeUtils.parseIso("not-a-date"));
    }

    @Test
    void testDurationMs_Success() {
        Instant start = Instant.ofEpochMilli(1000);
        Instant end = Instant.ofEpochMilli(5000);
        assertEquals(4000, TimeUtils.durationMs(start, end));
    }

    @Test
    void testDurationMs_NullStartOrEnd() {
        Instant instant = Instant.now();
        assertEquals(0, TimeUtils.durationMs(null, instant));
        assertEquals(0, TimeUtils.durationMs(instant, null));
        assertEquals(0, TimeUtils.durationMs(null, null));
    }

    @Test
    void testNowUtc() {
        ZonedDateTime now = TimeUtils.nowUtc();
        assertNotNull(now);
        assertEquals(ZoneId.of("UTC"), now.getZone());
    }

    @Test
    void testFormatReadable_Success() {
        ZonedDateTime dateTime = ZonedDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        String readable = TimeUtils.formatReadable(dateTime);
        assertEquals("2021-01-01 12:00:00 UTC", readable);
    }

    @Test
    void testFormatReadable_Null() {
        assertEquals("", TimeUtils.formatReadable(null));
    }
}