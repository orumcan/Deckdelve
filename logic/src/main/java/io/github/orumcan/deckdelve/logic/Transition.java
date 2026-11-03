package io.github.orumcan.deckdelve.logic;

import java.util.List;
import java.util.Objects;

public record Transition(GameState state, List<Event> events) {
    public Transition {
        Objects.requireNonNull(state, "state");
        events = List.copyOf(events);
    }
}
