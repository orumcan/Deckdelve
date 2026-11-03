package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.card;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StartTest {
    @Test
    void dealsTheFirstRoomFromTheTopOfTheDungeon() {
        Seed seed = Seed.parse("7KQ2-M9XD");
        Transition start = Rules.start(seed, RulesConfig.OFFICIAL);
        GameState state = start.state();

        assertEquals("[10♦, 10♣, 6♠, 5♥]", state.room().toString());
        assertEquals(Deck.shuffled(seed).subList(4, 44), state.dungeon());
        assertEquals(
                List.of(
                        new Event.CardDealt(0, card("10♦")),
                        new Event.CardDealt(1, card("10♣")),
                        new Event.CardDealt(2, card("6♠")),
                        new Event.CardDealt(3, card("5♥"))),
                start.events());
        assertEquals(seed, state.seed());
        assertEquals(20, state.health());
        assertEquals(Optional.empty(), state.weapon());
        assertEquals(List.of(), state.discard());
        assertEquals(Status.PLAYING, state.status());
    }

    @Test
    void theSameSeedDealsTheSameRun() {
        assertEquals(Rules.start(Seed.of(42), RulesConfig.OFFICIAL), Rules.start(Seed.of(42), RulesConfig.OFFICIAL));
    }

    @Test
    void differentSeedsDealDifferentRuns() {
        assertNotEquals(
                Rules.start(Seed.of(1), RulesConfig.OFFICIAL).state().dungeon(),
                Rules.start(Seed.of(2), RulesConfig.OFFICIAL).state().dungeon());
    }
}
