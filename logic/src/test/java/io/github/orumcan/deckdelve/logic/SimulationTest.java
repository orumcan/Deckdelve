package io.github.orumcan.deckdelve.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTest {
    private static final int GAMES = 2_000;
    private static final int MAX_ACTIONS = 200;
    private static final List<RulesConfig> RULES = List.of(
            RulesConfig.OFFICIAL,
            RulesConfig.OFFICIAL.withWeaponRule(WeaponRule.STRICTLY_LOWER),
            RulesConfig.OFFICIAL.withFinalRoom(FinalRoom.LEAVE_LAST_CARD),
            RulesConfig.OFFICIAL.withSubtractRoomMonsters(true).withFloorHealthAtZero(true));

    @Test
    void randomGamesKeepEveryInvariant() {
        for (int game = 0; game < GAMES; game++) {
            SplitMix64 player = new SplitMix64(game);
            Transition transition = Rules.start(Seed.of(game), RULES.get(game % RULES.size()));
            checkInvariants(transition.state());

            for (int actions = 0; !transition.state().isOver(); actions++) {
                assertTrue(actions < MAX_ACTIONS, "Every run ends");
                List<Action> legal = Rules.legalActions(transition.state());
                assertFalse(legal.isEmpty(), "A run in progress always has a legal action");
                transition = Rules.apply(transition.state(), legal.get(player.nextInt(legal.size())));
                checkInvariants(transition.state());
            }

            GameState end = transition.state();
            Event last = transition.events().get(transition.events().size() - 1);
            assertEquals(new Event.GameOver(end.status(), Scoring.score(end)), last);
            assertTrue(end.status() == Status.WON ? end.health() > 0 : end.health() <= 0);
        }
    }

    @Test
    void replayingTheSameActionsReplaysTheSameRun() {
        Seed seed = Seed.parse("7KQ2-M9XD");
        SplitMix64 player = new SplitMix64(123);
        GameState state = Rules.start(seed, RulesConfig.OFFICIAL).state();
        List<Action> actions = new ArrayList<>();
        while (!state.isOver()) {
            List<Action> legal = Rules.legalActions(state);
            Action action = legal.get(player.nextInt(legal.size()));
            actions.add(action);
            state = Rules.apply(state, action).state();
        }

        GameState replay = Rules.start(seed, RulesConfig.OFFICIAL).state();
        for (Action action : actions) {
            replay = Rules.apply(replay, action).state();
        }
        assertEquals(state, replay);
    }

    private static void checkInvariants(GameState state) {
        List<Card> cards = new ArrayList<>(state.dungeon());
        cards.addAll(state.room().cards());
        cards.addAll(state.discard());
        state.weapon().ifPresent(weapon -> {
            cards.add(weapon.card());
            cards.addAll(weapon.slain());
        });
        assertEquals(Deck.SIZE, cards.size(), "No card is lost or duplicated");
        assertEquals(new HashSet<>(Deck.ordered()), new HashSet<>(cards), "Every card is somewhere");

        assertTrue(state.health() <= Rules.maxHealth(state.rules()), "Health never goes over 20");
        if (!state.isOver()) {
            assertTrue(state.health() > 0, "A run in progress has health left");
        }
        if (state.lastRoomAvoided()) {
            assertFalse(Rules.canAvoid(state), "Two Rooms in a row can't be avoided");
        }
        state.weapon().ifPresent(weapon -> {
            List<Card> slain = weapon.slain();
            boolean strict = state.rules().weaponRule() == WeaponRule.STRICTLY_LOWER;
            for (int i = 1; i < slain.size(); i++) {
                int previous = slain.get(i - 1).value();
                int next = slain.get(i).value();
                assertTrue(
                        strict ? next < previous : next <= previous, "A weapon only fights what its last kill allows");
            }
        });
    }
}
