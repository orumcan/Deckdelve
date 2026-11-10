package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import io.github.orumcan.deckdelve.logic.Achievement;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.Career;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.IllegalActionException;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Seed;
import io.github.orumcan.deckdelve.logic.Suit;
import io.github.orumcan.deckdelve.logic.Transition;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** What the game remembers between sessions: settings, stats, achievements and the run in progress. */
final class Profile {
    private static final String TUTORIAL_OFFERED = "tutorialOffered";
    private static final String THEME = "theme";
    private static final String COMPACT_ROOM = "compactRoom";
    private static final String ANIMATION_SPEED = "animationSpeed";
    private static final String VOLUME = "volume";
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
    private static final String MODIFIER_LIMIT = "modifierLimit";
    private static final String BEST_SCORE = "bestScore";
    private static final String KILLED_BY = "killedBy.";
    private static final String SEED = "seed";
    private static final String ACTIONS = "actions";
    private static final String UNLOCKED = "unlocked";

    private final Preferences preferences = Gdx.app.getPreferences("io.github.orumcan.deckdelve");
    /** Kept apart, so saving after every action rewrites only the run, and dropping it clears the whole file. */
    private final Preferences run = Gdx.app.getPreferences("io.github.orumcan.deckdelve.run");

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

    boolean compactRoom() {
        return preferences.getBoolean(COMPACT_ROOM, false);
    }

    void compactRoom(boolean on) {
        preferences.putBoolean(COMPACT_ROOM, on);
        preferences.flush();
    }

    /** The volume of every sound, in percent. */
    int volume() {
        return preferences.getInteger(VOLUME, 80);
    }

    void volume(int percent) {
        preferences.putInteger(VOLUME, percent);
        preferences.flush();
    }

    AnimationSpeed animationSpeed() {
        String saved = preferences.getString(ANIMATION_SPEED, "");
        for (AnimationSpeed speed : AnimationSpeed.values()) {
            if (speed.name().equals(saved)) {
                return speed;
            }
        }
        return AnimationSpeed.NORMAL;
    }

    void animationSpeed(AnimationSpeed speed) {
        preferences.putString(ANIMATION_SPEED, speed.name());
        preferences.flush();
    }

    /**
     * The rule variants and modifiers picked in Settings, for every run but the tutorial. Modifiers past the limit the
     * player has earned are left out.
     */
    RulesConfig rules() {
        RulesConfig picked = readRules(preferences);
        return picked.withModifiers(career().allowed(picked.modifiers()));
    }

    void rules(RulesConfig rules) {
        writeRules(preferences, rules);
        preferences.flush();
    }

    /** The run in progress, unless there's none or it no longer replays. */
    Optional<SavedRun> savedRun() {
        if (!run.contains(SEED)) {
            return Optional.empty();
        }
        try {
            SavedRun saved = new SavedRun(
                    Seed.parse(run.getString(SEED)),
                    readRules(run),
                    SavedRun.actions(run.getString(ACTIONS, "")),
                    List.copyOf(names(
                            run.getString(UNLOCKED, ""), Achievement.values(), EnumSet.noneOf(Achievement.class))));
            List<Transition> history = saved.replay();
            return history.get(history.size() - 1).state().isOver() ? Optional.empty() : Optional.of(saved);
        } catch (IllegalArgumentException | IllegalActionException e) {
            return Optional.empty();
        }
    }

    void save(SavedRun saved) {
        run.putString(SEED, saved.seed().toString());
        writeRules(run, saved.rules());
        run.putString(ACTIONS, SavedRun.codes(saved.actions()));
        run.putString(UNLOCKED, names(saved.unlocked()));
        run.flush();
    }

    void dropRun() {
        run.clear();
        run.flush();
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
        int wins = preferences.getInteger(WINS, 0);
        Set<Modifier> wonWith =
                names(preferences.getString(WON_WITH, ""), Modifier.values(), EnumSet.noneOf(Modifier.class));
        int limit = preferences.contains(MODIFIER_LIMIT)
                ? preferences.getInteger(MODIFIER_LIMIT)
                : limitFromRecord(wins, wonWith);
        return new Career(wins, preferences.getInteger(STREAK, 0), wonWith, limit);
    }

    /**
     * A profile saved before the modifier limit existed starts from what its record shows: Masochist means a win with
     * three modifiers, and any modifier won with means a win with one.
     */
    private int limitFromRecord(int wins, Set<Modifier> wonWith) {
        if (wins == 0) {
            return 0;
        }
        if (achievements().contains(Achievement.MASOCHIST)) {
            return 4;
        }
        return wonWith.isEmpty() ? 1 : 2;
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
        preferences.putInteger(MODIFIER_LIMIT, career.modifierLimit());
        preferences.flush();
        return career.earned();
    }

    private static String key(Card card) {
        return card.rank() + "_" + card.suit().name();
    }

    private static RulesConfig readRules(Preferences from) {
        return new RulesConfig(
                from.getBoolean(STRICTLY_LOWER, false) ? WeaponRule.STRICTLY_LOWER : WeaponRule.LOWER_OR_EQUAL,
                from.getBoolean(LEAVE_LAST_CARD, false) ? FinalRoom.LEAVE_LAST_CARD : FinalRoom.RESOLVE_EVERY_CARD,
                from.getBoolean(SUBTRACT_ROOM_MONSTERS, false),
                from.getBoolean(FLOOR_HEALTH, false),
                names(from.getString(MODIFIERS, ""), Modifier.values(), EnumSet.noneOf(Modifier.class)));
    }

    private static void writeRules(Preferences to, RulesConfig rules) {
        to.putBoolean(STRICTLY_LOWER, rules.weaponRule() == WeaponRule.STRICTLY_LOWER);
        to.putBoolean(LEAVE_LAST_CARD, rules.finalRoom() == FinalRoom.LEAVE_LAST_CARD);
        to.putBoolean(SUBTRACT_ROOM_MONSTERS, rules.subtractRoomMonsters());
        to.putBoolean(FLOOR_HEALTH, rules.floorHealthAtZero());
        to.putString(MODIFIERS, names(rules.modifiers()));
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

    private static String names(Collection<? extends Enum<?>> values) {
        StringBuilder saved = new StringBuilder();
        for (Enum<?> value : values) {
            saved.append(saved.length() == 0 ? "" : ",").append(value.name());
        }
        return saved.toString();
    }
}
