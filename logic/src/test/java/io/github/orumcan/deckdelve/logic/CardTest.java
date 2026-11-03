package io.github.orumcan.deckdelve.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {
    @Test
    void printsRankAndSuit() {
        assertEquals("Q♠", new Card(Suit.SPADES, Card.QUEEN).toString());
        assertEquals("10♦", new Card(Suit.DIAMONDS, 10).toString());
        assertEquals("A♣", new Card(Suit.CLUBS, Card.ACE).toString());
    }

    @Test
    void parsesSymbolsAndLetters() {
        assertEquals(new Card(Suit.SPADES, Card.QUEEN), Card.parse("Q♠"));
        assertEquals(new Card(Suit.SPADES, Card.QUEEN), Card.parse("qs"));
        assertEquals(new Card(Suit.HEARTS, 10), Card.parse(" 10H "));
    }

    @Test
    void parsesEveryCardItPrints() {
        for (Card card : Deck.ordered()) {
            assertEquals(card, Card.parse(card.toString()));
        }
    }

    @Test
    void takesItsTypeFromItsSuit() {
        assertEquals(CardType.MONSTER, Card.parse("2♣").type());
        assertEquals(CardType.MONSTER, Card.parse("2♠").type());
        assertEquals(CardType.WEAPON, Card.parse("2♦").type());
        assertEquals(CardType.POTION, Card.parse("2♥").type());
    }

    @ParameterizedTest
    @ValueSource(strings = {"J♥", "A♦", "1♠", "15♣", "Q", "Q?", "X♠", ""})
    void rejectsCardsOutsideTheDeck(String text) {
        assertThrows(IllegalArgumentException.class, () -> Card.parse(text));
    }
}
