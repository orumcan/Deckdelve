package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MultiplierTextTest {
    @Test
    void decimalsDropTrailingZeros() {
        assertEquals("1", Ui.decimal(100));
        assertEquals("1.4", Ui.decimal(140));
        assertEquals("1.25", Ui.decimal(125));
        assertEquals("2.05", Ui.decimal(205));
    }
}
