package io.github.orumcan.deckdelve.logic;

/**
 * The opt-in challenges. Each multiplies a winning score by its multiplier, given in hundredths: 125 is ×1.25. The
 * multipliers come from bot runs: {@code console --bot 50000} prints them.
 */
public enum Modifier {
    /** Start at 15 health, which is also the cap for healing and for the potion bonus. */
    FRAGILE(140),
    /** Rooms can't be avoided. */
    NO_RETREAT(125),
    /** Potions heal 1 less, and the potion bonus is 1 less too. */
    WATERED_DOWN(140),
    /** A weapon breaks on its fourth kill and goes to the discard with its stack. */
    BRITTLE_BLADES(160),
    /** Each Room's newest card is dealt face down and turns over once the first card in the Room is resolved. */
    FOG(170),
    /** Every fight costs at least 1 health, even when the weapon is stronger than the monster. */
    THORNS(175),
    /** Every card in a Room has to be resolved, so nothing carries over to the next. */
    FULL_CLEAR(190),
    /** Every second Room costs 1 health as it's dealt, counting Rooms dealt after an avoid. */
    HUNGER(145);

    private final int hundredths;

    Modifier(int hundredths) {
        this.hundredths = hundredths;
    }

    public int hundredths() {
        return hundredths;
    }
}
