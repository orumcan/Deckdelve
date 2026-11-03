package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TestStates {
    private RulesConfig rules = RulesConfig.OFFICIAL;
    private List<Card> dungeon = List.of();
    private Card[] room = new Card[Room.SIZE];
    private int health = Rules.MAX_HEALTH;
    private Weapon weapon;
    private int resolvedInRoom;
    private boolean potionUsed;
    private boolean lastRoomAvoided;

    private TestStates() {}

    static TestStates state() {
        return new TestStates();
    }

    static Card card(String text) {
        return Card.parse(text);
    }

    static List<Card> cards(String text) {
        List<Card> cards = new ArrayList<>();
        for (String card : text.trim().split("\\s+")) {
            if (!card.isEmpty()) {
                cards.add(Card.parse(card));
            }
        }
        return cards;
    }

    static GameState play(GameState state, Action... actions) {
        for (Action action : actions) {
            state = Rules.apply(state, action).state();
        }
        return state;
    }

    /** The Room's slots from the left, such as {@code "5♦ - Q♠"}, where "-" and missing slots are empty. */
    TestStates room(String slots) {
        room = new Card[Room.SIZE];
        String[] texts = slots.trim().split("\\s+");
        for (int slot = 0; slot < texts.length; slot++) {
            room[slot] = texts[slot].equals("-") ? null : Card.parse(texts[slot]);
        }
        return this;
    }

    TestStates dungeon(String cards) {
        dungeon = cards(cards);
        return this;
    }

    TestStates health(int health) {
        this.health = health;
        return this;
    }

    TestStates weapon(String card) {
        return weapon(card, "");
    }

    TestStates weapon(String card, String slain) {
        weapon = new Weapon(Card.parse(card), cards(slain));
        return this;
    }

    TestStates rules(RulesConfig rules) {
        this.rules = rules;
        return this;
    }

    TestStates resolvedInRoom(int resolvedInRoom) {
        this.resolvedInRoom = resolvedInRoom;
        return this;
    }

    TestStates potionUsed() {
        potionUsed = true;
        return this;
    }

    TestStates lastRoomAvoided() {
        lastRoomAvoided = true;
        return this;
    }

    GameState build() {
        return new GameState(
                Seed.of(0),
                rules,
                dungeon,
                Room.of(room),
                health,
                Optional.ofNullable(weapon),
                List.of(),
                resolvedInRoom,
                potionUsed,
                lastRoomAvoided,
                Optional.empty(),
                Status.PLAYING);
    }
}
