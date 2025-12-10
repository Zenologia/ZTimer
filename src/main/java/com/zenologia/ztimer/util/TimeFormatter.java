package com.zenologia.ztimer.util;

public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * Format milliseconds into a string using a very small subset of patterns:
     * - "mm:ss" -> minutes:seconds (2-digit seconds)
     *
     * Only full seconds are supported; fractional seconds are ignored.
     */
    public static String formatMillis(long millis, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = "mm:ss";
        }
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if ("mm:ss".equalsIgnoreCase(pattern)) {
            return String.format("%d:%02d", minutes, seconds);
        }

        // Fallback to mm:ss format if unknown pattern
        return String.format("%d:%02d", minutes, seconds);
    }
}
