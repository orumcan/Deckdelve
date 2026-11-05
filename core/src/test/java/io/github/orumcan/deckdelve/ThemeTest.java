package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ThemeTest {
    @Test
    void shrinkingAveragesThePixelsEachNewPixelCovers() {
        float[] image = {
            1, 1, 0, 0,
            1, 1, 0, 0,
            0, 0, 0, 0,
            0, 0, 1, 0
        };
        assertArrayEquals(new float[] {1, 0, 0, 0.25f}, Theme.shrink(image, 4, 2));
    }

    @Test
    void aPixelSplitBetweenNewPixelsCountsInEachByItsShare() {
        float[] dot = new float[3 * 3];
        dot[4] = 1;
        float ninth = 1 / 9f;
        assertArrayEquals(new float[] {ninth, ninth, ninth, ninth}, Theme.shrink(dot, 3, 2), 1e-6f);
    }
}
