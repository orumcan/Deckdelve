package io.github.orumcan.deckdelve.console;

import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Seed;
import java.util.Optional;

record Options(Optional<Seed> seed, RulesConfig rules, boolean help) {
    static final String USAGE = """
            Usage: console [options]
              --seed CODE                play a specific dungeon, such as 7KQ2-M9XD
              --strictly-lower           weapons only fight monsters below their last kill
              --leave-last-card          win with one card left in the last room
              --subtract-room-monsters   deaths also subtract the monsters left in the room
              --floor-health             deaths count health below 0 as 0
              --help                     show this help
            """;

    static Options parse(String[] args) {
        Optional<Seed> seed = Optional.empty();
        RulesConfig rules = RulesConfig.OFFICIAL;
        boolean help = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> {
                    i++;
                    if (i == args.length) {
                        throw new IllegalArgumentException("--seed needs a code, such as 7KQ2-M9XD.");
                    }
                    seed = Optional.of(Seed.parse(args[i]));
                }
                case "--strictly-lower" -> rules = rules.withWeaponRule(WeaponRule.STRICTLY_LOWER);
                case "--leave-last-card" -> rules = rules.withFinalRoom(FinalRoom.LEAVE_LAST_CARD);
                case "--subtract-room-monsters" -> rules = rules.withSubtractRoomMonsters(true);
                case "--floor-health" -> rules = rules.withFloorHealthAtZero(true);
                case "--help", "-h" -> help = true;
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        return new Options(seed, rules, help);
    }
}
