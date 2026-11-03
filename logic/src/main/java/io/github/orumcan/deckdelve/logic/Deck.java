package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The 44-card Scoundrel deck: a standard deck without its jokers, red face cards and red aces. */
final class Deck {
    static final int SIZE = 44;

    private Deck() {}

    static List<Card> ordered() {
        List<Card> cards = new ArrayList<>(SIZE);
        for (Suit suit : Suit.values()) {
            for (int value = 2; value <= suit.highestValue(); value++) {
                cards.add(new Card(suit, value));
            }
        }
        return cards;
    }

    /**
     * Fisher–Yates over {@link #ordered()}. Every seed depends on both, down to the order of the {@link Suit}
     * constants, so none of it may change.
     */
    static List<Card> shuffled(Seed seed) {
        List<Card> cards = ordered();
        SplitMix64 random = new SplitMix64(seed.value());
        for (int i = cards.size() - 1; i > 0; i--) {
            Collections.swap(cards, i, random.nextInt(i + 1));
        }
        return cards;
    }
}
