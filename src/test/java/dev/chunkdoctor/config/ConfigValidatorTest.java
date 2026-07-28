package dev.chunkdoctor.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigValidatorTest {
    @Test
    void thresholdsAreClampedAndStrictlyOrdered() {
        assertArrayEquals(new int[]{98, 99, 100}, ConfigValidator.orderedThresholds(500, -1, 2));
    }

    @Test
    void unsafeExportDirectoriesFailClosed() {
        assertEquals("reports", ConfigValidator.safeRelativeDirectory("../outside"));
        assertEquals("reports", ConfigValidator.safeRelativeDirectory("C:\\secret"));
        assertEquals("daily_reports", ConfigValidator.safeRelativeDirectory("daily reports"));
    }

    @Test
    void nonFiniteDoubleUsesMinimum() {
        assertEquals(0.1, ConfigValidator.clampDouble(Double.NaN, 0.1, 10.0));
    }
}
