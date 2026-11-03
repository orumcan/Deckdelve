package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A mutable copy of a {@link GameState}, changed step by step while one action is applied. */
final class Draft {
    final Seed seed;
    final RulesConfig rules;
    final List<Card> dungeon;
    final Card[] room;
    final List<Card> discard;
    int health;
    Weapon weapon;
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
        health = Rules.MAX_HEALTH;
        status = Status.PLAYING;
    }

    Draft(GameState state) {
        seed = state.seed();
        rules = state.rules();
        dungeon = new ArrayList<>(state.dungeon());
        room = state.room().toArray();
        discard = new ArrayList<>(state.discard());
        health = state.health();
        weapon = state.weapon().orElse(null);
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
        for (int slot = 0; slot < Room.SIZE && !dungeon.isEmpty(); slot++) {
            if (room[slot] == null) {
                room[slot] = dungeon.remove(0);
                emit(new Event.CardDealt(slot, room[slot]));
            }
        }
        resolvedInRoom = 0;
        potionUsed = false;
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
                health,
                Optional.ofNullable(weapon),
                discard,
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
