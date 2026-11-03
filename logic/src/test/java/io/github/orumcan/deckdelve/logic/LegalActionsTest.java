package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Avoid;
import io.github.orumcan.deckdelve.logic.Action.Drink;
import io.github.orumcan.deckdelve.logic.Action.Equip;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegalActionsTest {
    @Test
    void listsEveryChoiceFromLeftToRight() {
        GameState state = state().weapon("7♦").room("5♦ Q♠ 3♥ -").dungeon("2♣").build();

        assertEquals(
                List.of(new Avoid(), new Equip(0), Fight.barehanded(1), Fight.withWeapon(1), new Drink(2)),
                Rules.legalActions(state));
    }

    @Test
    void leavesOutWeaponFightsTheWeaponCantTake() {
        GameState state =
                state().weapon("7♦", "4♣").room("5♣ 2♣ 3♣ 4♣").dungeon("6♣").build();
        List<Action> legal = Rules.legalActions(state);

        assertTrue(legal.contains(Fight.barehanded(0)));
        assertFalse(legal.contains(Fight.withWeapon(0)));
        assertTrue(legal.contains(Fight.withWeapon(3)));
    }

    @Test
    void rejectsActionsThatDontMatchTheCard() {
        GameState state = state().room("5♦ Q♠ 3♥ -").dungeon("2♣").build();

        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Equip(1)));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Drink(0)));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, Fight.barehanded(2)));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Equip(3)));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Equip(4)));
    }
}
