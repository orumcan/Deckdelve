package io.github.orumcan.deckdelve.logic;

import java.util.List;
import java.util.Set;

public final class Scoring {
    private Scoring() {}

    /** The plain score, times the modifiers' multiplier on a win, rounded down. A death keeps its plain score. */
    public static int score(GameState state) {
        int plain = plainScore(state);
        return state.status() == Status.WON ? multiplied(plain, state.rules().modifiers()) : plain;
    }

    /**
     * The score before any multiplier. On a win, the remaining health, plus the potion's healing if the run ended at
     * full health on a potion. On a death, the health left minus every monster still in the Dungeon.
     */
    public static int plainScore(GameState state) {
        return switch (state.status()) {
            case WON -> state.health() + potionBonus(state);
            case LOST -> deathScore(state);
            case PLAYING -> throw new IllegalStateException("The run isn't over yet.");
        };
    }

    /** The modifiers' combined multiplier in hundredths, rounded to the nearest: ×1.4 and ×1.75 make 245. */
    public static int multiplier(Set<Modifier> modifiers) {
        Fraction product = product(modifiers);
        return (int) ((200 * product.numerator() + product.denominator()) / (2 * product.denominator()));
    }

    static int multiplied(int score, Set<Modifier> modifiers) {
        Fraction product = product(modifiers);
        return (int) Math.floorDiv(score * product.numerator(), product.denominator());
    }

    private record Fraction(long numerator, long denominator) {}

    /** Kept in lowest terms as it grows: in hundredths, every modifier at once would overflow a long. */
    private static Fraction product(Set<Modifier> modifiers) {
        long numerator = 1;
        long denominator = 1;
        for (Modifier modifier : modifiers) {
            numerator *= modifier.hundredths();
            denominator *= 100;
            long common = gcd(numerator, denominator);
            numerator /= common;
            denominator /= common;
        }
        return new Fraction(numerator, denominator);
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static int potionBonus(GameState state) {
        if (state.health() < Rules.maxHealth(state.rules())) {
            return 0;
        }
        return state.lastResolved()
                .filter(card -> card.type() == CardType.POTION)
                .map(potion -> Rules.healing(potion, state.rules()))
                .orElse(0);
    }

    private static int deathScore(GameState state) {
        RulesConfig rules = state.rules();
        int health = rules.floorHealthAtZero() ? Math.max(0, state.health()) : state.health();
        int monsters = monsterValue(state.dungeon());
        if (rules.subtractRoomMonsters()) {
            monsters += monsterValue(state.room().cards());
        }
        return health - monsters;
    }

    private static int monsterValue(List<Card> cards) {
        int total = 0;
        for (Card card : cards) {
            if (card.type() == CardType.MONSTER) {
                total += card.value();
            }
        }
        return total;
    }
}
