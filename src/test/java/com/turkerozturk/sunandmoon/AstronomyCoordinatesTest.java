package com.turkerozturk.sunandmoon;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AstronomyCoordinatesTest {
    @Test void retainsFractionalSecondsAndDirections() {
        assertThat(AstronomyCoordinates.parse("40°59'21.5\"N", true)).isCloseTo(40.98930556, within(0.00000001));
        assertThat(AstronomyCoordinates.parse("29°02'14.4\"W", false)).isCloseTo(-29.03733333, within(0.00000001));
        assertThat(AstronomyCoordinates.parse("33°30'0\"S", true)).isEqualTo(-33.5);
    }
    @Test void acceptsSignedDecimalAndNegativeZeroDms() {
        assertThat(AstronomyCoordinates.parse("-33.5", true)).isEqualTo(-33.5);
        assertThat(AstronomyCoordinates.parse("+151.2", false)).isEqualTo(151.2);
        assertThat(AstronomyCoordinates.parse("-0°30'0\"", true)).isEqualTo(-0.5);
    }
    @Test void rejectsAmbiguousAndInvalidCoordinates() {
        for (String value : new String[]{"", "N", "NaN", "91", "40°60'0\"N", "40°0'60\"N", "-33S", "40E", "40,5", "90°0'1\""}) {
            assertThatThrownBy(() -> AstronomyCoordinates.parse(value, true)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> AstronomyCoordinates.parse("181", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AstronomyCoordinates.parse(null, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
