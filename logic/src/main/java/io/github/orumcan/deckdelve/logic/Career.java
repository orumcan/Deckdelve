package io.github.orumcan.deckdelve.logic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A player's record across runs, for the achievements no single run can earn. */
public record Career(int wins, int streak, Set<Modifier> wonWith) {
    private static final int HAT_TRICK = 3;
    private static final int VETERAN = 10;

    public Career {
        EnumSet<Modifier> copy = EnumSet.noneOf(Modifier.class);
        copy.addAll(wonWith);
        wonWith = Collections.unmodifiableSet(copy);
    }

    public static Career start() {
        return new Career(0, 0, Set.of());
    }

    /** The record after a run that counts ends, or is left after its first card: anything but a win ends the streak. */
    public Career after(GameState run) {
        if (run.status() != Status.WON) {
            return new Career(wins, 0, wonWith);
        }
        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        modifiers.addAll(wonWith);
        modifiers.addAll(run.rules().modifiers());
        return new Career(wins + 1, streak + 1, modifiers);
    }

    public Set<Achievement> earned() {
        EnumSet<Achievement> earned = EnumSet.noneOf(Achievement.class);
        if (streak >= HAT_TRICK) {
            earned.add(Achievement.HAT_TRICK);
        }
        if (wins >= VETERAN) {
            earned.add(Achievement.VETERAN);
        }
        if (wonWith.size() == Modifier.values().length) {
            earned.add(Achievement.CHALLENGER);
        }
        return earned;
    }
}
