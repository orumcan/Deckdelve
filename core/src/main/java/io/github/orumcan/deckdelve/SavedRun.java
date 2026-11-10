package io.github.orumcan.deckdelve;

import io.github.orumcan.deckdelve.logic.Achievement;
import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Room;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;
import io.github.orumcan.deckdelve.logic.Transition;
import java.util.ArrayList;
import java.util.List;

/**
 * A run as the game keeps it between sessions: the seed, rules and actions that replay it exactly (D18), and the
 * achievements it unlocked for the first time, which its end lists.
 */
record SavedRun(Seed seed, RulesConfig rules, List<Action> actions, List<Achievement> unlocked) {
    SavedRun {
        actions = List.copyOf(actions);
        unlocked = List.copyOf(unlocked);
    }

    static SavedRun start(Seed seed, RulesConfig rules) {
        return new SavedRun(seed, rules, List.of(), List.of());
    }

    /**
     * Plays the run again from its seed: the start, then one transition per action. Throws IllegalActionException if
     * an action no longer applies, as it can after a rules change.
     */
    List<Transition> replay() {
        List<Transition> history = new ArrayList<>();
        Transition last = Rules.start(seed, rules);
        history.add(last);
        for (Action action : actions) {
            last = Rules.apply(last.state(), action);
            history.add(last);
        }
        return history;
    }

    /**
     * Writes actions as short codes, such as {@code A,E0,W2,D1,B3}: avoid, equip, fight with the weapon, drink and
     * fight barehanded, each followed by its Room slot.
     */
    static String codes(List<Action> actions) {
        StringBuilder codes = new StringBuilder();
        for (Action action : actions) {
            codes.append(codes.length() == 0 ? "" : ",").append(code(action));
        }
        return codes.toString();
    }

    /** Reads codes written by {@link #codes}, and throws IllegalArgumentException on anything else. */
    static List<Action> actions(String codes) {
        List<Action> actions = new ArrayList<>();
        if (!codes.isEmpty()) {
            for (String code : codes.split(",")) {
                actions.add(action(code));
            }
        }
        return actions;
    }

    private static String code(Action action) {
        if (action instanceof Action.Equip equip) {
            return "E" + equip.slot();
        }
        if (action instanceof Action.Drink drink) {
            return "D" + drink.slot();
        }
        if (action instanceof Action.Fight fight) {
            return (fight.useWeapon() ? "W" : "B") + fight.slot();
        }
        return "A";
    }

    private static Action action(String code) {
        if (code.equals("A")) {
            return new Action.Avoid();
        }
        if (code.length() != 2 || code.charAt(1) < '0' || code.charAt(1) >= '0' + Room.SIZE) {
            throw new IllegalArgumentException("Not an action: " + code);
        }
        int slot = code.charAt(1) - '0';
        return switch (code.charAt(0)) {
            case 'E' -> new Action.Equip(slot);
            case 'D' -> new Action.Drink(slot);
            case 'W' -> Action.Fight.withWeapon(slot);
            case 'B' -> Action.Fight.barehanded(slot);
            default -> throw new IllegalArgumentException("Not an action: " + code);
        };
    }
}
