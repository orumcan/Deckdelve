package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.card;
import static io.github.orumcan.deckdelve.logic.TestStates.cards;
import static io.github.orumcan.deckdelve.logic.TestStates.play;
import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Avoid;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomRulesTest {
    @Test
    void aRoomEndsWithOneCardLeftWhichKeepsItsSlot() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").dungeon("6♣ 7♣ 8♣ 9♣ 10♣").build();
        GameState afterTwo = play(state, Fight.barehanded(1), Fight.barehanded(0));
        Transition third = Rules.apply(afterTwo, Fight.barehanded(3));
        GameState next = third.state();

        assertEquals("[6♣, 7♣, 4♣, 8♣]", next.room().toString());
        assertEquals(cards("9♣ 10♣"), next.dungeon());
        assertEquals(10, next.health());
        assertEquals(0, next.resolvedInRoom());
        assertEquals(
                List.of(
                        new Event.MonsterFought(card("5♣"), false, 5),
                        new Event.CardDealt(0, card("6♣")),
                        new Event.CardDealt(1, card("7♣")),
                        new Event.CardDealt(3, card("8♣"))),
                third.events());
    }

    @Test
    void avoidingSendsTheRoomToTheBottomInSlotOrder() {
        GameState state = state().room("2♣ 3♥ 4♦ 5♠").dungeon("6♣ 7♣ 8♣ 9♣ 10♣").build();
        Transition avoid = Rules.apply(state, new Avoid());

        assertEquals("[6♣, 7♣, 8♣, 9♣]", avoid.state().room().toString());
        assertEquals(cards("10♣ 2♣ 3♥ 4♦ 5♠"), avoid.state().dungeon());
        assertTrue(avoid.state().lastRoomAvoided());
        assertEquals(new Event.RoomAvoided(cards("2♣ 3♥ 4♦ 5♠")), avoid.events().get(0));
        assertEquals(5, avoid.events().size());
    }

    @Test
    void twoRoomsInARowCantBeAvoided() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").dungeon("6♣ 7♣ 8♣ 9♣ 10♣").build();
        GameState avoided = Rules.apply(state, new Avoid()).state();

        assertFalse(Rules.canAvoid(avoided));
        assertThrows(IllegalActionException.class, () -> Rules.apply(avoided, new Avoid()));
    }

    @Test
    void facingARoomAllowsAvoidingAgain() {
        GameState state = state().lastRoomAvoided()
                .room("2♣ 3♣ 4♣ 5♣")
                .dungeon("6♣ 7♣ 8♣ 9♣")
                .build();
        GameState next = play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));

        assertTrue(Rules.canAvoid(next));
    }

    @Test
    void aRoomCantBeAvoidedOnceACardIsResolved() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").dungeon("6♣").build();
        GameState started = Rules.apply(state, Fight.barehanded(0)).state();

        assertThrows(IllegalActionException.class, () -> Rules.apply(started, new Avoid()));
    }

    @Test
    void theFirstRoomCanBeAvoided() {
        assertTrue(Rules.canAvoid(Rules.start(Seed.of(1), RulesConfig.OFFICIAL).state()));
    }

    @Test
    void theLastRoomCantBeAvoided() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").build();

        assertFalse(Rules.canAvoid(state));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Avoid()));
    }

    @Test
    void aShortDungeonDealsAPartialLastRoom() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").dungeon("6♣").build();
        GameState last = play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));

        assertEquals("[6♣, -, -, 5♣]", last.room().toString());
        assertTrue(last.dungeon().isEmpty());
    }
}
