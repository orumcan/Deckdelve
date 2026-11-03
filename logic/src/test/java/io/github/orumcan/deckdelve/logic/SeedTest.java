package io.github.orumcan.deckdelve.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SeedTest {
    private static final long MAX = (1L << Seed.BITS) - 1;

    @Test
    void writesTwoGroupsOfFourCharacters() {
        assertEquals("0000-0000", new Seed(0).toString());
        assertEquals("0000-0001", new Seed(1).toString());
        assertEquals("014S-C0PJ", new Seed(1_234_567_890).toString());
        assertEquals("ZZZZ-ZZZZ", new Seed(MAX).toString());
    }

    @Test
    void readsWhatItWrites() {
        assertEquals(261_693_777_837L, Seed.parse("7KQ2-M9XD").value());
        for (long value : new long[] {0, 1, 1_234_567_890, 261_693_777_837L, MAX}) {
            assertEquals(new Seed(value), Seed.parse(new Seed(value).toString()));
        }
    }

    @Test
    void forgivesCaseMissingHyphensAndLookalikeLetters() {
        Seed seed = Seed.parse("014S-C0PJ");
        assertEquals(seed, Seed.parse("014sc0pj"));
        assertEquals(seed, Seed.parse("O14S-COPJ"));
        assertEquals(new Seed(1), Seed.parse("0000-000I"));
        assertEquals(new Seed(1), Seed.parse("0000-000l"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "014S-C0P", "014S-C0PJ1", "014S-C0PU", "014S C0PJ"})
    void rejectsMalformedSeeds(String text) {
        assertThrows(IllegalArgumentException.class, () -> Seed.parse(text));
    }

    @Test
    void keepsTheLowFortyBitsOfAnyNumber() {
        assertEquals(new Seed(5), Seed.of(1L << Seed.BITS | 5));
        assertEquals(new Seed(MAX), Seed.of(-1));
    }

    @Test
    void rejectsValuesWiderThanFortyBits() {
        assertThrows(IllegalArgumentException.class, () -> new Seed(MAX + 1));
        assertThrows(IllegalArgumentException.class, () -> new Seed(-1));
    }
}
