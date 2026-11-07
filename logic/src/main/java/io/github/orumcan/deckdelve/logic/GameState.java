package io.github.orumcan.deckdelve.logic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A snapshot of a run. The dungeon lists its top card first, the discard its oldest first; health can go below 0. Under
 * Fog, faceDown is the Room slot whose card hasn't turned over yet. Rooms counts every Room dealt so far, the first
 * included.
 */
public record GameState(
        Seed seed,
        RulesConfig rules,
        List<Card> dungeon,
        Room room,
        OptionalInt faceDown,
        int health,
        Optional<Weapon> weapon,
        List<Card> discard,
        int rooms,
        int resolvedInRoom,
        boolean potionUsed,
        boolean lastRoomAvoided,
        Optional<Card> lastResolved,
        Status status) {

    public GameState {
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(faceDown, "faceDown");
        Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(lastResolved, "lastResolved");
        Objects.requireNonNull(status, "status");
        dungeon = List.copyOf(dungeon);
        discard = List.copyOf(discard);
    }

    public boolean isOver() {
        return status != Status.PLAYING;
    }

    public boolean isFaceDown(int slot) {
        return faceDown.isPresent() && faceDown.getAsInt() == slot;
    }
}
