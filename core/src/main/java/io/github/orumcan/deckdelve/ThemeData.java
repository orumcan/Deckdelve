package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.Suit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A theme's theme.json: its name, a tint for each suit, a name and an icon for every card, the three cards the title
 * screen fans out, and its credits.
 */
record ThemeData(
        Localized name, Map<Suit, Color> tints, Map<Card, Entry> cards, List<Card> title, List<Localized> credits) {
    /** A weapon, a potion and a monster, the monster in front; for themes that don't name their own. */
    static final List<Card> DEFAULT_TITLE =
            List.of(new Card(Suit.DIAMONDS, 10), new Card(Suit.HEARTS, 7), new Card(Suit.SPADES, Card.ACE));

    private static final List<CardType> TITLE_TYPES = List.of(CardType.WEAPON, CardType.POTION, CardType.MONSTER);

    record Entry(Localized name, String icon) {}

    /** Mapped by hand rather than by reflection, which TeaVM can't do. */
    static ThemeData parse(JsonValue json) {
        Map<Suit, Color> tints = new HashMap<>();
        JsonValue colors = json.get("tints");
        for (Suit suit : Suit.values()) {
            String tint = colors.getString(key(suit), null);
            if (tint == null) {
                throw new IllegalArgumentException("No tint for " + key(suit));
            }
            tints.put(suit, Color.valueOf(tint));
        }

        Map<Card, Entry> cards = new HashMap<>();
        for (JsonValue card : json.get("cards")) {
            cards.put(Card.parse(card.name), new Entry(Localized.of(card.get("name")), card.getString("icon")));
        }
        for (Suit suit : Suit.values()) {
            for (int value = 2; value <= suit.highestValue(); value++) {
                Card card = new Card(suit, value);
                if (!cards.containsKey(card)) {
                    throw new IllegalArgumentException("No entry for " + card);
                }
            }
        }

        List<Localized> credits = new ArrayList<>();
        JsonValue lines = json.get("credits");
        if (lines != null) {
            for (JsonValue line : lines) {
                credits.add(Localized.of(line));
            }
        }
        return new ThemeData(
                Localized.of(json.get("name")),
                Collections.unmodifiableMap(tints),
                Collections.unmodifiableMap(cards),
                title(json.get("title")),
                Collections.unmodifiableList(credits));
    }

    private static List<Card> title(JsonValue json) {
        if (json == null) {
            return DEFAULT_TITLE;
        }
        List<Card> cards = new ArrayList<>();
        for (JsonValue card : json) {
            cards.add(Card.parse(card.asString()));
        }
        List<CardType> types = new ArrayList<>();
        for (Card card : cards) {
            types.add(card.type());
        }
        if (!types.equals(TITLE_TYPES)) {
            throw new IllegalArgumentException("The title takes a weapon, a potion and a monster, in that order");
        }
        return Collections.unmodifiableList(cards);
    }

    private static String key(Suit suit) {
        return switch (suit) {
            case CLUBS -> "clubs";
            case SPADES -> "spades";
            case DIAMONDS -> "diamonds";
            case HEARTS -> "hearts";
        };
    }
}
