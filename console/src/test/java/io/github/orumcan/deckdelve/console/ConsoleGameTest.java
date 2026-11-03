package io.github.orumcan.deckdelve.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Seed;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ConsoleGameTest {
    private static final Seed SEED = Seed.parse("7KQ2-M9XD");

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Test
    void quittingAbandonsTheRun() throws IOException {
        GameState state = play("q\n");

        assertFalse(state.isOver());
        assertTrue(output().contains("seed 7KQ2-M9XD"));
        assertTrue(output().contains("Run abandoned"));
    }

    @Test
    void theRunStopsWhenTheInputEnds() throws IOException {
        assertFalse(play("").isOver());
    }

    @Test
    void asksAgainAfterAMistake() throws IOException {
        play("9\nhello\nq\n");
        assertTrue(output().contains("Type 1-4, A or Q."));
    }

    @Test
    void playsAWholeRunFromTypedCommands() throws IOException {
        Random player = new Random(1);
        GameState expected = Rules.start(SEED, RulesConfig.OFFICIAL).state();
        StringBuilder input = new StringBuilder();
        while (!expected.isOver()) {
            List<Action> legal = Rules.legalActions(expected);
            Action action = legal.get(player.nextInt(legal.size()));
            input.append(command(action)).append('\n');
            expected = Rules.apply(expected, action).state();
        }

        assertEquals(expected, play(input.toString()));
        assertTrue(output().contains("Score: " + Scoring.score(expected) + "."));
    }

    private GameState play(String input) throws IOException {
        PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
        return new ConsoleGame(new BufferedReader(new StringReader(input)), out).play(SEED, RulesConfig.OFFICIAL);
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    private static String command(Action action) {
        if (action instanceof Action.Avoid) {
            return "a";
        }
        if (action instanceof Action.Equip equip) {
            return String.valueOf(equip.slot() + 1);
        }
        if (action instanceof Action.Drink drink) {
            return String.valueOf(drink.slot() + 1);
        }
        Action.Fight fight = (Action.Fight) action;
        return (fight.slot() + 1) + (fight.useWeapon() ? "w" : "b");
    }
}
