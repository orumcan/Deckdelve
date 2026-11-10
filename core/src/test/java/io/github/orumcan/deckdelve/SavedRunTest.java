package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.IllegalActionException;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Transition;
import java.util.List;
import org.junit.jupiter.api.Test;

class SavedRunTest {
    private static final List<Action> TUTORIAL_OPENING = List.of(
            new Action.Equip(1),
            Action.Fight.withWeapon(2),
            Action.Fight.withWeapon(0),
            new Action.Drink(2),
            new Action.Drink(1));

    @Test
    void everyKindOfActionHasAShortCode() {
        List<Action> actions = List.of(
                new Action.Avoid(),
                new Action.Equip(0),
                Action.Fight.withWeapon(2),
                new Action.Drink(1),
                Action.Fight.barehanded(3));
        assertEquals("A,E0,W2,D1,B3", SavedRun.codes(actions));
        assertEquals(actions, SavedRun.actions("A,E0,W2,D1,B3"));
    }

    @Test
    void aRunWithNoActionsHasNoCodes() {
        assertEquals("", SavedRun.codes(List.of()));
        assertEquals(List.of(), SavedRun.actions(""));
    }

    @Test
    void anythingElseIsNotAnAction() {
        for (String codes : List.of("X1", "E4", "W", "e0", "A,,D1", "D1 ")) {
            assertThrows(IllegalArgumentException.class, () -> SavedRun.actions(codes), codes);
        }
    }

    @Test
    void replayingRebuildsTheSameRun() {
        GameState played = Rules.start(Tutorial.SEED, RulesConfig.OFFICIAL).state();
        for (Action action : TUTORIAL_OPENING) {
            played = Rules.apply(played, action).state();
        }
        SavedRun saved = new SavedRun(
                Tutorial.SEED, RulesConfig.OFFICIAL, SavedRun.actions(SavedRun.codes(TUTORIAL_OPENING)), List.of());
        List<Transition> history = saved.replay();
        assertEquals(TUTORIAL_OPENING.size() + 1, history.size());
        assertEquals(played, history.get(history.size() - 1).state());
    }

    @Test
    void anActionThatNoLongerAppliesStopsTheReplay() {
        SavedRun twoAvoids = new SavedRun(
                Tutorial.SEED, RulesConfig.OFFICIAL, List.of(new Action.Avoid(), new Action.Avoid()), List.of());
        assertThrows(IllegalActionException.class, twoAvoids::replay);
    }
}
