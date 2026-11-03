package io.github.orumcan.deckdelve.logic;

import java.util.Objects;

/** The rule variants players can pick in Settings. The two booleans only change how a death is scored. */
public record RulesConfig(
        WeaponRule weaponRule, FinalRoom finalRoom, boolean subtractRoomMonsters, boolean floorHealthAtZero) {

    public static final RulesConfig OFFICIAL =
            new RulesConfig(WeaponRule.LOWER_OR_EQUAL, FinalRoom.RESOLVE_EVERY_CARD, false, false);

    public enum WeaponRule {
        LOWER_OR_EQUAL,
        STRICTLY_LOWER
    }

    public enum FinalRoom {
        RESOLVE_EVERY_CARD,
        LEAVE_LAST_CARD
    }

    public RulesConfig {
        Objects.requireNonNull(weaponRule, "weaponRule");
        Objects.requireNonNull(finalRoom, "finalRoom");
    }

    public RulesConfig withWeaponRule(WeaponRule weaponRule) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero);
    }

    public RulesConfig withFinalRoom(FinalRoom finalRoom) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero);
    }

    public RulesConfig withSubtractRoomMonsters(boolean subtractRoomMonsters) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero);
    }

    public RulesConfig withFloorHealthAtZero(boolean floorHealthAtZero) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero);
    }
}
