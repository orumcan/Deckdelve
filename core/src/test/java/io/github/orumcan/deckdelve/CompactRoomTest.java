package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Room;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompactRoomTest {
    @Test
    void theCardLeftFromTheLastRoomComesFirst() {
        Room room = Room.of(card("Q♠"), card("6♥"), card("8♥"), card("5♥"));
        assertArrayEquals(new int[] {3, 0, 1, 2}, TableScreen.compactOrder(room, List.of(0, 1, 2)));
    }

    @Test
    void newCardsFollowInTheOrderTheyWereDealt() {
        Room room = Room.of(card("2♣"), card("J♠"), card("4♦"), card("9♥"));
        assertArrayEquals(new int[] {1, 0, 2, 3}, TableScreen.compactOrder(room, List.of(0, 2, 3)));
    }

    @Test
    void aRoomDealtWholeKeepsItsSlotOrder() {
        Room room = Room.of(card("2♣"), card("J♠"), card("4♦"), card("9♥"));
        assertArrayEquals(new int[] {0, 1, 2, 3}, TableScreen.compactOrder(room, List.of(0, 1, 2, 3)));
    }

    @Test
    void theLastRoomsEmptySlotsComeLast() {
        Room room = Room.of(card("7♣"), card("3♥"), card("K♠"), null);
        assertArrayEquals(new int[] {2, 0, 1, 3}, TableScreen.compactOrder(room, List.of(0, 1)));
    }

    private static Card card(String text) {
        return Card.parse(text);
    }
}
