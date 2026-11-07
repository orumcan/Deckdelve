package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** A mutable copy of a {@link GameState}, changed step by step while one action is applied. */
final class Draft {
    private static final int NO_SLOT = -1;

    final Seed seed;
    final RulesConfig rules;
    final List<Card> dungeon;
    final Card[] room;
    final List<Card> discard;
    int faceDown = NO_SLOT;
    int health;
    Weapon weapon;
    int rooms;
    int resolvedInRoom;
    boolean potionUsed;
    boolean lastRoomAvoided;
    Card lastResolved;
    Status status;

    private final List<Event> events = new ArrayList<>();

    Draft(Seed seed, RulesConfig rules, List<Card> deck) {
        this.seed = seed;
        this.rules = rules;
        dungeon = new ArrayList<>(deck);
        room = new Card[Room.SIZE];
        discard = new ArrayList<>();
        health = Rules.maxHealth(rules);
        status = Status.PLAYING;
    }

    Draft(GameState state) {
        seed = state.seed();
        rules = state.rules();
        dungeon = new ArrayList<>(state.dungeon());
        room = state.room().toArray();
        faceDown = state.faceDown().orElse(NO_SLOT);
        discard = new ArrayList<>(state.discard());
        health = state.health();
        weapon = state.weapon().orElse(null);
        rooms = state.rooms();
        resolvedInRoom = state.resolvedInRoom();
        potionUsed = state.potionUsed();
        lastRoomAvoided = state.lastRoomAvoided();
        lastResolved = state.lastResolved().orElse(null);
        status = state.status();
    }

    Card resolve(int slot) {
        Card card = room[slot];
        room[slot] = null;
        resolvedInRoom++;
        lastResolved = card;
        return card;
    }

    void dealRoom() {
        List<Integer> dealt = new ArrayList<>();
        for (int slot = 0; slot < Room.SIZE && !dungeon.isEmpty(); slot++) {
            if (room[slot] == null) {
                room[slot] = dungeon.remove(0);
                dealt.add(slot);
            }
        }
        if (!dealt.isEmpty()) {
            rooms++;
        }
        faceDown = rules.has(Modifier.FOG) && !dealt.isEmpty() ? dealt.get(dealt.size() - 1) : NO_SLOT;
        for (int slot : dealt) {
            emit(new Event.CardDealt(slot, room[slot], slot == faceDown));
        }
        resolvedInRoom = 0;
        potionUsed = false;
    }

    /** Turns the face-down card over once the first card in its Room is resolved. */
    void revealFaceDown() {
        if (faceDown != NO_SLOT && resolvedInRoom > 0) {
            emit(new Event.CardRevealed(faceDown, room[faceDown]));
            faceDown = NO_SLOT;
        }
    }

    void clearFaceDown() {
        faceDown = NO_SLOT;
    }

    void discard(Weapon weapon) {
        discard.add(weapon.card());
        discard.addAll(weapon.slain());
    }

    int roomCount() {
        int count = 0;
        for (Card card : room) {
            if (card != null) {
                count++;
            }
        }
        return count;
    }

    void emit(Event event) {
        events.add(event);
    }

    Transition finish() {
        GameState state = new GameState(
                seed,
                rules,
                dungeon,
                Room.of(room),
                faceDown == NO_SLOT ? OptionalInt.empty() : OptionalInt.of(faceDown),
                health,
                Optional.ofNullable(weapon),
                discard,
                rooms,
                resolvedInRoom,
                potionUsed,
                lastRoomAvoided,
                Optional.ofNullable(lastResolved),
                status);
        if (state.isOver()) {
            emit(new Event.GameOver(status, Scoring.score(state)));
        }
        return new Transition(state, events);
    }
}
