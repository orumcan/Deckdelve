package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.play;
import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.orumcan.deckdelve.logic.Action.Drink;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EndOfRunTest {
    @Test
    void theLastRoomHasToBeFinishedByDefault() {
        GameState state = state().room("2♣ 3♣ 4♣ 5♣").build();
        GameState oneLeft = play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));
        assertEquals(Status.PLAYING, oneLeft.status());

        Transition last = Rules.apply(oneLeft, Fight.barehanded(3));
        assertEquals(Status.WON, last.state().status());
        assertEquals(new Event.GameOver(Status.WON, 6), lastEvent(last));
    }

    @Test
    void theLeaveLastCardVariantWinsWithOneCardLeft() {
        RulesConfig rules = RulesConfig.OFFICIAL.withFinalRoom(FinalRoom.LEAVE_LAST_CARD);
        GameState state = state().rules(rules).room("2♣ 3♣ 4♣ 5♣").build();
        GameState end = play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));

        assertEquals(Status.WON, end.status());
        assertEquals("[-, -, -, 5♣]", end.room().toString());
        assertEquals(11, Scoring.score(end));
    }

    @Test
    void theRunEndsTheMomentHealthReachesZero() {
        GameState state =
                state().health(5).room("Q♠ 2♣ 3♣ 4♣").dungeon("K♠ 7♥ 9♣").build();
        Transition death = Rules.apply(state, Fight.barehanded(0));

        assertEquals(Status.LOST, death.state().status());
        assertEquals(-7, death.state().health());
        assertEquals(List.of(), Rules.legalActions(death.state()));
        assertThrows(IllegalActionException.class, () -> Rules.apply(death.state(), Fight.barehanded(1)));
        // -7 health, minus the K♠ and 9♣ still in the Dungeon.
        assertEquals(new Event.GameOver(Status.LOST, -29), lastEvent(death));
    }

    @ParameterizedTest(name = "subtract Room monsters: {0}, floor health at 0: {1}")
    @CsvSource({"false, false, -29", "true, false, -38", "false, true, -22", "true, true, -31"})
    void deathScoreVariants(boolean subtractRoomMonsters, boolean floorHealthAtZero, int score) {
        RulesConfig rules = RulesConfig.OFFICIAL
                .withSubtractRoomMonsters(subtractRoomMonsters)
                .withFloorHealthAtZero(floorHealthAtZero);
        GameState state = state().rules(rules)
                .health(5)
                .room("Q♠ 2♣ 3♣ 4♣")
                .dungeon("K♠ 7♥ 9♣")
                .build();

        assertEquals(
                score, Scoring.score(Rules.apply(state, Fight.barehanded(0)).state()));
    }

    @Test
    void survivingScoresTheHealthLeft() {
        GameState state = state().health(12).resolvedInRoom(3).room("- - - 2♣").build();
        assertEquals(10, Scoring.score(Rules.apply(state, Fight.barehanded(3)).state()));
    }

    @Test
    void endingOnAPotionAtFullHealthAddsItsValue() {
        GameState state = state().health(18).resolvedInRoom(3).room("5♥").build();
        GameState end = Rules.apply(state, new Drink(0)).state();

        assertEquals(Status.WON, end.status());
        assertEquals(25, Scoring.score(end));
    }

    @Test
    void aWastedPotionAtTheEndStillEarnsTheBonus() {
        GameState state = state().potionUsed().resolvedInRoom(3).room("4♥").build();
        assertEquals(24, Scoring.score(Rules.apply(state, new Drink(0)).state()));
    }

    @Test
    void endingBelowFullHealthEarnsNoBonus() {
        GameState state = state().health(10).resolvedInRoom(3).room("5♥").build();
        assertEquals(15, Scoring.score(Rules.apply(state, new Drink(0)).state()));
    }

    @Test
    void endingOnAMonsterEarnsNoBonus() {
        GameState state = state().weapon("5♦").resolvedInRoom(3).room("3♣").build();
        assertEquals(20, Scoring.score(Rules.apply(state, Fight.withWeapon(0)).state()));
    }

    @Test
    void onlyFinishedRunsHaveAScore() {
        GameState state = state().room("2♣").dungeon("3♣").build();
        assertThrows(IllegalStateException.class, () -> Scoring.score(state));
    }

    private static Event lastEvent(Transition transition) {
        return transition.events().get(transition.events().size() - 1);
    }
}
