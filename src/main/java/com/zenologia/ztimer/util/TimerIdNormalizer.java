package com.zenologia.ztimer.util;

public final class TimerIdNormalizer {

    private TimerIdNormalizer() {
    }

    /**
     * Normalize a timer ID according to the spec:
     * - lowercase
     * - keep only [a-z0-9_-]
     * - return null if result is empty
     */
    public static String normalize(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }
}
