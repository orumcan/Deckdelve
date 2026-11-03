package io.github.orumcan.deckdelve.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Seed;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OptionsTest {
    @Test
    void defaultsToTheOfficialRulesAndARandomSeed() {
        Options options = Options.parse(new String[0]);

        assertEquals(RulesConfig.OFFICIAL, options.rules());
        assertTrue(options.seed().isEmpty());
    }

    @Test
    void readsTheSeedAndEveryRuleVariant() {
        Options options = Options.parse(new String[] {
            "--seed", "7kq2-m9xd", "--strictly-lower", "--leave-last-card", "--subtract-room-monsters", "--floor-health"
        });

        assertEquals(Optional.of(Seed.parse("7KQ2-M9XD")), options.seed());
        assertEquals(
                new RulesConfig(WeaponRule.STRICTLY_LOWER, FinalRoom.LEAVE_LAST_CARD, true, true), options.rules());
    }

    @Test
    void rejectsUnknownOptionsAndBadSeeds() {
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--fast"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seed"}));
        assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] {"--seed", "nope"}));
    }
}
