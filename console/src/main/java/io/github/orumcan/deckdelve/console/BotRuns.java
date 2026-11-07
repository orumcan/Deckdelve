package io.github.orumcan.deckdelve.console;

import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;
import io.github.orumcan.deckdelve.logic.Status;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Set;
import java.util.stream.LongStream;

/**
 * Plays the same seeds with no modifier and with each one. Each multiplier is the cube root of how many times harder
 * the modifier makes winning, rounded to the nearest 0.05: the bot is much weaker than people, who feel each handicap
 * less.
 */
final class BotRuns {
    private BotRuns() {}

    static void run(int games, RulesConfig rules, PrintStream out) {
        double baseline = winRate(games, rules);
        out.println("Bot runs: " + games + " seeds per line, seeds 1 to " + games + ".");
        out.println(String.format(
                Locale.ROOT, "%-15s %8s %9s %10s %8s", "Modifier", "Win rate", "Harder", "Suggested", "Now"));
        out.println(String.format(Locale.ROOT, "%-15s %7.1f%%", "none", 100 * baseline));
        for (Modifier modifier : Modifier.values()) {
            double rate = winRate(games, rules.withModifiers(Set.of(modifier)));
            double harder = baseline / rate;
            double suggested = Math.round(Math.cbrt(harder) * 20) / 20.0;
            out.println(String.format(
                    Locale.ROOT,
                    "%-15s %7.1f%% %8.2fx %9.2fx %7.2fx",
                    modifier,
                    100 * rate,
                    harder,
                    suggested,
                    modifier.hundredths() / 100.0));
        }
    }

    static double winRate(int games, RulesConfig rules) {
        long wins = LongStream.rangeClosed(1, games)
                .parallel()
                .filter(seed -> play(Seed.of(seed), rules) == Status.WON)
                .count();
        return wins / (double) games;
    }

    static Status play(Seed seed, RulesConfig rules) {
        GameState state = Rules.start(seed, rules).state();
        while (!state.isOver()) {
            state = Rules.apply(state, Bot.choose(state)).state();
        }
        return state.status();
    }
}
