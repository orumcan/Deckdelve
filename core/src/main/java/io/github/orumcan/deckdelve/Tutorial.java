package io.github.orumcan.deckdelve;

import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.Event;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.Seed;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Picks the tutorial's tips. Each tip appears once, at the first moment it applies. */
final class Tutorial {
    /**
     * Opens with a modest weapon, two soft monsters and a potion; the next room brings two potions and a queen the
     * weapon can't fight, and the third a better weapon.
     */
    static final Seed SEED = Seed.parse("A9B9-GK2D");

    /** In priority order: when two tips apply at once, the earlier one shows and the other waits. */
    enum Tip {
        ROOM("tutorial.room"),
        AVOIDED("tutorial.avoided"),
        WASTE("tutorial.waste"),
        KILL("tutorial.kill"),
        WEAPON("tutorial.weapon"),
        AVOID("tutorial.avoid");

        final String key;

        Tip(String key) {
            this.key = key;
        }
    }

    private final boolean[] shown = new boolean[Tip.values().length];

    Optional<Tip> start(GameState state) {
        return next(state, List.of());
    }

    Optional<Tip> next(GameState state, List<Event> events) {
        if (state.isOver()) {
            return Optional.empty();
        }
        for (Tip tip : Tip.values()) {
            if (!shown[tip.ordinal()] && applies(tip, state, events)) {
                shown[tip.ordinal()] = true;
                return Optional.of(tip);
            }
        }
        return Optional.empty();
    }

    private static boolean applies(Tip tip, GameState state, List<Event> events) {
        return switch (tip) {
            case ROOM -> events.isEmpty();
            case AVOIDED -> happened(events, event -> event instanceof Event.RoomAvoided);
            case WASTE -> state.potionUsed() && inRoom(state, CardType.POTION);
            case KILL -> state.weapon().map(weapon -> !weapon.slain().isEmpty()).orElse(false);
            case WEAPON ->
                inRoom(state, CardType.WEAPON) || happened(events, event -> event instanceof Event.WeaponEquipped);
            case AVOID -> happened(events, event -> event instanceof Event.CardDealt) && Rules.canAvoid(state);
        };
    }

    private static boolean inRoom(GameState state, CardType type) {
        for (Card card : state.room().cards()) {
            if (card.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean happened(List<Event> events, Predicate<Event> kind) {
        for (Event event : events) {
            if (kind.test(event)) {
                return true;
            }
        }
        return false;
    }
}
