package io.github.orumcan.deckdelve.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SplitMix64Test {
    @Test
    void matchesThePublishedReferenceOutputs() {
        SplitMix64 random = new SplitMix64(0);
        assertEquals(0xE220A8397B1DCDAFL, random.nextLong());
        assertEquals(0x6E789E6AA1B965F4L, random.nextLong());
        assertEquals(0x06C45D188009454FL, random.nextLong());
    }

    /** Pins nextInt: if this fails, every existing seed now deals a different dungeon. */
    @Test
    void drawsTheSameNumbersForever() {
        SplitMix64 random = new SplitMix64(0);
        int[] draws = new int[5];
        for (int i = 0; i < draws.length; i++) {
            draws[i] = random.nextInt(44);
        }
        assertArrayEquals(new int[] {32, 5, 20, 16, 5}, draws);
    }

    @Test
    void drawsEveryNumberInRangeEvenly() {
        SplitMix64 random = new SplitMix64(7);
        int[] counts = new int[6];
        for (int i = 0; i < 60_000; i++) {
            counts[random.nextInt(counts.length)]++;
        }
        for (int count : counts) {
            assertTrue(Math.abs(count - 10_000) < 500, () -> Arrays.toString(counts));
        }
    }

    @Test
    void rejectsBoundsBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new SplitMix64(0).nextInt(0));
    }
}
