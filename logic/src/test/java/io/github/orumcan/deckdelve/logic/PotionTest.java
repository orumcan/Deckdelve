package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.card;
import static io.github.orumcan.deckdelve.logic.TestStates.cards;
import static io.github.orumcan.deckdelve.logic.TestStates.play;
import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.orumcan.deckdelve.logic.Action.Drink;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.util.List;
import org.junit.jupiter.api.Test;

class PotionTest {
    @Test
    void aPotionHealsByItsValue() {
        GameState state = state().health(10).room("7♥ 2♣ 3♣ 4♣").dungeon("5♣").build();
        Transition drink = Rules.apply(state, new Drink(0));

        assertEquals(17, drink.state().health());
        assertEquals(cards("7♥"), drink.state().discard());
        assertEquals(List.of(new Event.PotionDrunk(card("7♥"), 7)), drink.events());
    }

    @Test
    void healingStopsAtTwenty() {
        GameState state = state().health(15).room("9♥ 2♣ 3♣ 4♣").dungeon("5♣").build();
        Transition drink = Rules.apply(state, new Drink(0));

        assertEquals(20, drink.state().health());
        assertEquals(List.of(new Event.PotionDrunk(card("9♥"), 5)), drink.events());
    }

    @Test
    void aSecondPotionInTheSameTurnIsWasted() {
        GameState state = state().health(10).room("3♥ 5♥ 2♣ 9♣").dungeon("5♣").build();
        GameState first = Rules.apply(state, new Drink(0)).state();
        Transition second = Rules.apply(first, new Drink(1));

        assertEquals(13, second.state().health());
        assertEquals(cards("3♥ 5♥"), second.state().discard());
        assertEquals(List.of(new Event.PotionWasted(card("5♥"))), second.events());
    }

    @Test
    void drinkingAtFullHealthStillUsesUpTheTurnsPotion() {
        GameState state = state().room("3♥ 5♥ 2♣ 9♣").dungeon("5♣").build();
        Transition first = Rules.apply(state, new Drink(0));

        assertEquals(List.of(new Event.PotionDrunk(card("3♥"), 0)), first.events());
        assertEquals(
                List.of(new Event.PotionWasted(card("5♥"))),
                Rules.apply(first.state(), new Drink(1)).events());
    }

    @Test
    void theNextRoomBringsAFreshPotion() {
        GameState state =
                state().health(10).room("3♥ 2♣ 4♣ 5♥").dungeon("8♣ 9♣ 10♣ J♣").build();
        GameState next = play(state, new Drink(0), Fight.barehanded(1), Fight.barehanded(2));

        assertEquals("[8♣, 9♣, 10♣, 5♥]", next.room().toString());
        assertFalse(next.potionUsed());
        assertEquals(12, Rules.apply(next, new Drink(3)).state().health());
    }
}
