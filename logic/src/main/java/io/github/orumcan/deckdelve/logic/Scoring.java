package io.github.orumcan.deckdelve.logic;

import java.util.List;

public final class Scoring {
    private Scoring() {}

    /**
     * On a win, the remaining health, plus the potion's value if the run ended at full health on a potion. On a
     * death, the health left minus every monster still in the Dungeon.
     */
    public static int score(GameState state) {
        return switch (state.status()) {
            case WON -> state.health() + potionBonus(state);
            case LOST -> deathScore(state);
            case PLAYING -> throw new IllegalStateException("The run isn't over yet.");
        };
    }

    private static int potionBonus(GameState state) {
        if (state.health() < Rules.MAX_HEALTH) {
            return 0;
        }
        return state.lastResolved()
                .filter(card -> card.type() == CardType.POTION)
                .map(Card::value)
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
