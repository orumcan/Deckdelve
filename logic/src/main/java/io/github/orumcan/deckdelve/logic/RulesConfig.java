package io.github.orumcan.deckdelve.logic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The rules a run is played by: the rule variants and the modifiers players pick in Settings. The two booleans only
 * change how a death is scored.
 */
public record RulesConfig(
        WeaponRule weaponRule,
        FinalRoom finalRoom,
        boolean subtractRoomMonsters,
        boolean floorHealthAtZero,
        Set<Modifier> modifiers) {

    public static final RulesConfig OFFICIAL =
            new RulesConfig(WeaponRule.LOWER_OR_EQUAL, FinalRoom.RESOLVE_EVERY_CARD, false, false, Set.of());

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
        EnumSet<Modifier> copy = EnumSet.noneOf(Modifier.class);
        copy.addAll(modifiers);
        modifiers = Collections.unmodifiableSet(copy);
    }

    public boolean has(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    public RulesConfig withWeaponRule(WeaponRule weaponRule) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero, modifiers);
    }

    public RulesConfig withFinalRoom(FinalRoom finalRoom) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero, modifiers);
    }

    public RulesConfig withSubtractRoomMonsters(boolean subtractRoomMonsters) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero, modifiers);
    }

    public RulesConfig withFloorHealthAtZero(boolean floorHealthAtZero) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero, modifiers);
    }

    public RulesConfig withModifiers(Set<Modifier> modifiers) {
        return new RulesConfig(weaponRule, finalRoom, subtractRoomMonsters, floorHealthAtZero, modifiers);
    }
}
