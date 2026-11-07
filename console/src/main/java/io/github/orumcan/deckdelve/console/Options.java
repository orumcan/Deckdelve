package io.github.orumcan.deckdelve.console;

import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Seed;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

record Options(Optional<Seed> seed, RulesConfig rules, int botGames, boolean help) {
    static final String USAGE = """
            Usage: console [options]
              --seed CODE                play a specific dungeon, such as 7KQ2-M9XD
              --strictly-lower           weapons only fight monsters below their last kill
              --leave-last-card          win with one card left in the last room
              --subtract-room-monsters   deaths also subtract the monsters left in the room
              --floor-health             deaths count health below 0 as 0
              --fragile                  start at 15 health, which is also the cap
              --no-retreat               rooms can't be avoided
              --watered-down             potions heal 1 less
              --brittle-blades           weapons break on their fourth kill
              --fog                      each room's newest card is dealt face down
              --thorns                   every fight costs at least 1 health
              --full-clear               every card in a room has to be taken
              --hunger                   each room after the first costs 1 health
              --bot GAMES                let a bot play GAMES seeds with each modifier and
                                         print the win rates, to tune the multipliers
              --help                     show this help
            """;

    static Options parse(String[] args) {
        Optional<Seed> seed = Optional.empty();
        RulesConfig rules = RulesConfig.OFFICIAL;
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        int botGames = 0;
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
                case "--fragile" -> modifiers.add(Modifier.FRAGILE);
                case "--no-retreat" -> modifiers.add(Modifier.NO_RETREAT);
                case "--watered-down" -> modifiers.add(Modifier.WATERED_DOWN);
                case "--brittle-blades" -> modifiers.add(Modifier.BRITTLE_BLADES);
                case "--fog" -> modifiers.add(Modifier.FOG);
                case "--thorns" -> modifiers.add(Modifier.THORNS);
                case "--full-clear" -> modifiers.add(Modifier.FULL_CLEAR);
                case "--hunger" -> modifiers.add(Modifier.HUNGER);
                case "--bot" -> {
                    i++;
                    if (i == args.length || !args[i].matches("[1-9][0-9]{0,6}")) {
                        throw new IllegalArgumentException("--bot needs a number of games, such as 10000.");
                    }
                    botGames = Integer.parseInt(args[i]);
                }
                case "--help", "-h" -> help = true;
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        return new Options(seed, rules.withModifiers(modifiers), botGames, help);
    }
}
