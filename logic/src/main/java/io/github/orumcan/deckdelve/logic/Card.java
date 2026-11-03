package io.github.orumcan.deckdelve.logic;

import java.util.Objects;

public record Card(Suit suit, int value) {
    public static final int JACK = 11;
    public static final int QUEEN = 12;
    public static final int KING = 13;
    public static final int ACE = 14;

    public Card {
        Objects.requireNonNull(suit, "suit");
        if (value < 2 || value > suit.highestValue()) {
            throw new IllegalArgumentException(suit + " run from 2 to " + suit.highestValue() + ", not " + value);
        }
    }

    /** Reads a card such as {@code Q♠}, {@code 10♦} or {@code qs}. */
    public static Card parse(String text) {
        String card = text.trim();
        if (card.length() < 2) {
            throw new IllegalArgumentException("Not a card: " + text);
        }
        Suit suit = Suit.of(card.charAt(card.length() - 1));
        String rank = card.substring(0, card.length() - 1);
        int value = switch (rank) {
            case "J", "j" -> JACK;
            case "Q", "q" -> QUEEN;
            case "K", "k" -> KING;
            case "A", "a" -> ACE;
            default -> number(rank, text);
        };
        return new Card(suit, value);
    }

    public CardType type() {
        return suit.type();
    }

    public String rank() {
        return switch (value) {
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
            case ACE -> "A";
            default -> Integer.toString(value);
        };
    }

    @Override
    public String toString() {
        return rank() + suit.symbol();
    }

    private static int number(String rank, String text) {
        try {
            return Integer.parseInt(rank);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a card: " + text, e);
        }
    }
}
