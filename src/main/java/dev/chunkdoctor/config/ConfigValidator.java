package dev.chunkdoctor.config;

/**
 * Pure validation helpers used by ConfigLoader and unit tests.
 */
public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static long clampLong(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static double clampDouble(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static int[] orderedThresholds(int medium, int high, int critical) {
        int safeMedium = clampInt(medium, 1, 98);
        int safeHigh = clampInt(high, safeMedium + 1, 99);
        int safeCritical = clampInt(critical, safeHigh + 1, 100);
        return new int[]{safeMedium, safeHigh, safeCritical};
    }

    public static String safeRelativeDirectory(String candidate) {
        if (candidate == null || candidate.isBlank()
                || candidate.contains("..")
                || candidate.contains("/")
                || candidate.contains("\\")
                || candidate.contains(":")) {
            return "reports";
        }
        return candidate.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
