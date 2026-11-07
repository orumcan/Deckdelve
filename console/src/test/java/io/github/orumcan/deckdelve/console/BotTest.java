package io.github.orumcan.deckdelve.console;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;
import io.github.orumcan.deckdelve.logic.Status;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class BotTest {
    @Test
    void playsWholeRunsWithEveryModifierOn() {
        RulesConfig rules = RulesConfig.OFFICIAL.withModifiers(EnumSet.allOf(Modifier.class));
        for (long seed = 1; seed <= 50; seed++) {
            assertNotEquals(Status.PLAYING, BotRuns.play(Seed.of(seed), rules));
        }
    }
}
