package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.cards;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeckTest {
    @Test
    void holds44DifferentCards() {
        List<Card> deck = Deck.ordered();
        assertEquals(44, deck.size());
        assertEquals(44, new HashSet<>(deck).size());
        assertEquals(26, count(deck, CardType.MONSTER));
        assertEquals(9, count(deck, CardType.WEAPON));
        assertEquals(9, count(deck, CardType.POTION));
    }

    @Test
    void shufflingKeepsEveryCard() {
        List<Card> shuffled = Deck.shuffled(Seed.of(99));
        assertEquals(44, shuffled.size());
        assertEquals(new HashSet<>(Deck.ordered()), new HashSet<>(shuffled));
    }

    /** Pins the shuffle: if this fails, every existing seed now deals a different dungeon. */
    @Test
    void eachSeedDealsTheSameDungeonForever() {
        assertEquals(
                cards("K♣ J♣ 8♣ 4♣ 9♦ 6♣ 3♥ 7♥ 5♦ 2♥ 4♦ 6♠ 10♦ 7♠ 6♦ Q♠ 9♠ 8♠ 3♣ 8♥ 2♣ A♠ "
                        + "J♠ 7♣ 4♠ 3♦ 9♥ 10♥ 5♥ 4♥ 5♣ 7♦ 3♠ Q♣ A♣ K♠ 9♣ 2♠ 10♣ 10♠ 5♠ 2♦ 6♥ 8♦"),
                Deck.shuffled(Seed.parse("0000-0000")));
        assertEquals(
                cards("10♦ 10♣ 6♠ 5♥ 9♦ 2♠ 8♠ 4♠ J♠ A♠ 9♥ 5♠ 10♥ 8♥ 9♠ 10♠ 5♦ 6♥ 9♣ Q♣ K♣ 4♣ "
                        + "3♠ 3♦ 2♣ 7♦ 7♠ 8♦ A♣ 2♥ 4♥ 8♣ 7♣ 2♦ 7♥ K♠ J♣ 3♥ 5♣ Q♠ 3♣ 6♦ 4♦ 6♣"),
                Deck.shuffled(Seed.parse("7KQ2-M9XD")));
    }

    private static long count(List<Card> cards, CardType type) {
        return cards.stream().filter(card -> card.type() == type).count();
    }
}
