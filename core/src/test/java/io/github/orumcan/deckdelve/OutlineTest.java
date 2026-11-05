package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OutlineTest {
    private static final int SIZE = 21;
    private static final int MIDDLE = SIZE / 2;

    @Test
    void outlinesAHoleWithoutFillingIt() {
        float[] ring = new float[SIZE * SIZE];
        for (int y = 6; y <= 14; y++) {
            for (int x = 6; x <= 14; x++) {
                boolean edge = x == 6 || x == 14 || y == 6 || y == 14;
                ring[y * SIZE + x] = edge ? 1 : 0;
            }
        }
        float[] outline = Outline.of(ring, SIZE, 2);
        assertEquals(1, at(outline, 7, MIDDLE));
        assertEquals(0, at(outline, MIDDLE, MIDDLE));
    }

    @Test
    void reachesTheRadiusPastTheIconWithASoftRim() {
        float[] dot = new float[SIZE * SIZE];
        dot[MIDDLE * SIZE + MIDDLE] = 1;
        float[] outline = Outline.of(dot, SIZE, 4);
        assertEquals(1, at(outline, MIDDLE + 3, MIDDLE));
        assertEquals(0.5f, at(outline, MIDDLE, MIDDLE + 4), 1e-6);
        assertEquals(0, at(outline, MIDDLE + 5, MIDDLE));
    }

    @Test
    void anEmptyIconHasNoOutline() {
        float[] outline = Outline.of(new float[SIZE * SIZE], SIZE, 4);
        for (float coverage : outline) {
            assertEquals(0, coverage);
        }
    }

    private static float at(float[] coverage, int x, int y) {
        return coverage[y * SIZE + x];
    }
}
