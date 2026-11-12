package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafeAreaTest {
    private static final float TOLERANCE = 0.001f;

    @Test
    void aScreenWithoutCutoutsKeepsTheLayoutsOwnWorld() {
        SafeArea safe = SafeArea.of(1080, 1920, 0, 0, 0, 0);

        assertEquals(1.5f, safe.pixelsPerUnit(), TOLERANCE);
        assertEquals(0, safe.top(), TOLERANCE);
        assertEquals(Ui.WIDTH, safe.minWorldWidth(), TOLERANCE);
        assertEquals(Ui.HEIGHT, safe.minWorldHeight(), TOLERANCE);
    }

    @Test
    void aTallPhoneGivesTheCutoutItsRoomWithoutShrinkingAnything() {
        // A Pixel 7: 1080 by 2400, with 136 pixels at the top to keep clear of the camera.
        SafeArea safe = SafeArea.of(1080, 2400, 0, 136, 0, 0);

        assertEquals(1.5f, safe.pixelsPerUnit(), TOLERANCE);
        assertEquals(136 / 1.5f, safe.top(), TOLERANCE);
        assertEquals(0, safe.bottom(), TOLERANCE);
    }

    @Test
    void aShortScreenShrinksUntilTheLayoutsFitBesideTheCutout() {
        SafeArea safe = SafeArea.of(1200, 1600, 0, 100, 0, 0);

        assertEquals(1500 / Ui.HEIGHT, safe.pixelsPerUnit(), TOLERANCE);
        assertFitsInside(safe, 1200, 1600, 0, 100, 0, 0);
    }

    @Test
    void insetsOnEverySideAllTakeTheirRoom() {
        SafeArea safe = SafeArea.of(1500, 2400, 60, 120, 30, 90);

        assertFitsInside(safe, 1500, 2400, 60, 120, 30, 90);
    }

    /**
     * The layouts' world fits in what the insets leave, and the world for the whole screen comes out at the same
     * scale: an extending viewport scales by the tighter of its two smallest sides.
     */
    private static void assertFitsInside(
            SafeArea safe, int width, int height, int left, int top, int right, int bottom) {
        float scale = safe.pixelsPerUnit();
        assertTrue((width - left - right) / scale >= Ui.WIDTH - TOLERANCE);
        assertTrue((height - top - bottom) / scale >= Ui.HEIGHT - TOLERANCE);
        assertEquals(scale, Math.min(width / safe.minWorldWidth(), height / safe.minWorldHeight()), TOLERANCE);
        assertEquals(top, safe.top() * scale, TOLERANCE);
        assertEquals(left, safe.left() * scale, TOLERANCE);
        assertEquals(right, safe.right() * scale, TOLERANCE);
        assertEquals(bottom, safe.bottom() * scale, TOLERANCE);
    }
}
