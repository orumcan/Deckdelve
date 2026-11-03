package io.github.orumcan.deckdelve.logic;

public enum Suit {
    CLUBS('♣', 'C', CardType.MONSTER),
    SPADES('♠', 'S', CardType.MONSTER),
    DIAMONDS('♦', 'D', CardType.WEAPON),
    HEARTS('♥', 'H', CardType.POTION);

    private final char symbol;
    private final char letter;
    private final CardType type;

    Suit(char symbol, char letter, CardType type) {
        this.symbol = symbol;
        this.letter = letter;
        this.type = type;
    }

    public char symbol() {
        return symbol;
    }

    public CardType type() {
        return type;
    }

    public int highestValue() {
        return type == CardType.MONSTER ? Card.ACE : 10;
    }

    static Suit of(char c) {
        for (Suit suit : values()) {
            if (suit.symbol == c || suit.letter == Character.toUpperCase(c)) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Not a suit: " + c);
    }
}
