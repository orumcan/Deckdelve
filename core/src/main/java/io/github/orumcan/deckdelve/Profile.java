package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import io.github.orumcan.deckdelve.logic.Achievement;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.Career;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Suit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** What the game remembers between sessions: settings, stats and achievements. */
final class Profile {
    private static final String TUTORIAL_OFFERED = "tutorialOffered";
    private static final String THEME = "theme";
    private static final String STRICTLY_LOWER = "strictlyLower";
    private static final String LEAVE_LAST_CARD = "leaveLastCard";
    private static final String SUBTRACT_ROOM_MONSTERS = "subtractRoomMonsters";
    private static final String FLOOR_HEALTH = "floorHealth";
    private static final String MODIFIERS = "modifiers";
    private static final String ACHIEVEMENTS = "achievements";
    private static final String RUNS = "runs";
    private static final String WINS = "wins";
    private static final String STREAK = "streak";
    private static final String WON_WITH = "wonWith";
    private static final String BEST_SCORE = "bestScore";
    private static final String KILLED_BY = "killedBy.";

    private final Preferences preferences = Gdx.app.getPreferences("io.github.orumcan.deckdelve");

    boolean tutorialOffered() {
        return preferences.getBoolean(TUTORIAL_OFFERED, false);
    }

    void markTutorialOffered() {
        preferences.putBoolean(TUTORIAL_OFFERED, true);
        preferences.flush();
    }

    String theme() {
        return preferences.getString(THEME, Themes.DEFAULT);
    }

    void theme(String id) {
        preferences.putString(THEME, id);
        preferences.flush();
    }

    /** The rule variants and modifiers picked in Settings, for every run but the tutorial. */
    RulesConfig rules() {
        return new RulesConfig(
                preferences.getBoolean(STRICTLY_LOWER, false) ? WeaponRule.STRICTLY_LOWER : WeaponRule.LOWER_OR_EQUAL,
                preferences.getBoolean(LEAVE_LAST_CARD, false)
                        ? FinalRoom.LEAVE_LAST_CARD
                        : FinalRoom.RESOLVE_EVERY_CARD,
                preferences.getBoolean(SUBTRACT_ROOM_MONSTERS, false),
                preferences.getBoolean(FLOOR_HEALTH, false),
                names(preferences.getString(MODIFIERS, ""), Modifier.values(), EnumSet.noneOf(Modifier.class)));
    }

    void rules(RulesConfig rules) {
        preferences.putBoolean(STRICTLY_LOWER, rules.weaponRule() == WeaponRule.STRICTLY_LOWER);
        preferences.putBoolean(LEAVE_LAST_CARD, rules.finalRoom() == FinalRoom.LEAVE_LAST_CARD);
        preferences.putBoolean(SUBTRACT_ROOM_MONSTERS, rules.subtractRoomMonsters());
        preferences.putBoolean(FLOOR_HEALTH, rules.floorHealthAtZero());
        preferences.putString(MODIFIERS, names(rules.modifiers()));
        preferences.flush();
    }

    Set<Achievement> achievements() {
        return names(preferences.getString(ACHIEVEMENTS, ""), Achievement.values(), EnumSet.noneOf(Achievement.class));
    }

    /** Keeps the achievement, and returns whether this is its first unlock. */
    boolean unlock(Achievement achievement) {
        Set<Achievement> unlocked = achievements();
        if (!unlocked.add(achievement)) {
            return false;
        }
        preferences.putString(ACHIEVEMENTS, names(unlocked));
        preferences.flush();
        return true;
    }

    int runs() {
        return preferences.getInteger(RUNS, 0);
    }

    Career career() {
        return new Career(
                preferences.getInteger(WINS, 0),
                preferences.getInteger(STREAK, 0),
                names(preferences.getString(WON_WITH, ""), Modifier.values(), EnumSet.noneOf(Modifier.class)));
    }

    Optional<Integer> bestScore() {
        return preferences.contains(BEST_SCORE) ? Optional.of(preferences.getInteger(BEST_SCORE)) : Optional.empty();
    }

    /** The monster that has ended the most runs; the strongest wins a tie. */
    Optional<Card> nemesis() {
        Card nemesis = null;
        int most = 0;
        for (Suit suit : Suit.values()) {
            if (suit.type() != CardType.MONSTER) {
                continue;
            }
            for (int value = 2; value <= suit.highestValue(); value++) {
                Card card = new Card(suit, value);
                int deaths = deathsTo(card);
                if (deaths > most || deaths == most && deaths > 0 && value > nemesis.value()) {
                    nemesis = card;
                    most = deaths;
                }
            }
        }
        return Optional.ofNullable(nemesis);
    }

    int deathsTo(Card monster) {
        return preferences.getInteger(KILLED_BY + key(monster), 0);
    }

    /**
     * Counts a run as it ends, or as it's left once its first card is resolved, and returns the career achievements the
     * record now earns. A death names the monster that dealt the last blow, unless Hunger did.
     */
    Set<Achievement> record(GameState run, Optional<Card> killer) {
        preferences.putInteger(RUNS, runs() + 1);
        if (run.isOver()) {
            int score = Scoring.score(run);
            if (bestScore().map(best -> score > best).orElse(true)) {
                preferences.putInteger(BEST_SCORE, score);
            }
        }
        killer.ifPresent(monster -> preferences.putInteger(KILLED_BY + key(monster), deathsTo(monster) + 1));
        Career career = career().after(run);
        preferences.putInteger(WINS, career.wins());
        preferences.putInteger(STREAK, career.streak());
        preferences.putString(WON_WITH, names(career.wonWith()));
        preferences.flush();
        return career.earned();
    }

    private static String key(Card card) {
        return card.rank() + "_" + card.suit().name();
    }

    private static <E extends Enum<E>> Set<E> names(String saved, E[] all, Set<E> values) {
        for (String name : saved.split(",")) {
            for (E value : all) {
                if (value.name().equals(name)) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String names(Set<? extends Enum<?>> values) {
        StringBuilder saved = new StringBuilder();
        for (Enum<?> value : values) {
            saved.append(saved.length() == 0 ? "" : ",").append(value.name());
        }
        return saved.toString();
    }
}
