package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The face-up cards in play: four fixed slots, numbered 0 to 3 from the left, any of which can be empty. A card keeps
 * its slot until it's resolved, and new cards fill the empty slots from the left.
 */
public final class Room {
    public static final int SIZE = 4;

    private static final Room EMPTY = new Room(new Card[SIZE]);

    private final Card[] slots;

    private Room(Card[] slots) {
        this.slots = slots;
    }

    public static Room empty() {
        return EMPTY;
    }

    /** Builds a room from its four slots, left to right, with {@code null} for an empty slot. */
    public static Room of(Card... slots) {
        if (slots.length != SIZE) {
            throw new IllegalArgumentException("A room has " + SIZE + " slots, not " + slots.length);
        }
        return new Room(slots.clone());
    }

    public Optional<Card> card(int slot) {
        if (slot < 0 || slot >= SIZE) {
            throw new IndexOutOfBoundsException("Slots run from 0 to " + (SIZE - 1) + ", not " + slot);
        }
        return Optional.ofNullable(slots[slot]);
    }

    public List<Card> cards() {
        List<Card> cards = new ArrayList<>(SIZE);
        for (Card card : slots) {
            if (card != null) {
                cards.add(card);
            }
        }
        return Collections.unmodifiableList(cards);
    }

    public int count() {
        int count = 0;
        for (Card card : slots) {
            if (card != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    Card[] toArray() {
        return slots.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Room room && Arrays.equals(slots, room.slots);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(slots);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("[");
        for (int slot = 0; slot < SIZE; slot++) {
            text.append(slot == 0 ? "" : ", ").append(slots[slot] == null ? "-" : slots[slot]);
        }
        return text.append(']').toString();
    }
}
