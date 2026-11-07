package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What a run has done that achievements look at, built up one transition at a time. Some achievements are earned
 * mid-run; the rest when the run is won. The ones that span several runs are in {@link Career}.
 */
public record Progress(
        Optional<Card> lastEquipped,
        int risingEquips,
        int barehandedKills,
        boolean aceSlainBarehanded,
        int acesSlainWithWeapon,
        int avoids,
        boolean potionWasted,
        int cleanKillsInRoom,
        Set<Achievement> earned) {

    private static final int ARSENAL = 3;
    private static final int STACK = 5;
    private static final int FLAWLESS = 3;
    private static final int BARE_KNUCKLES = 7;
    private static final int ESCAPE_ARTIST = 5;
    private static final int MASOCHIST = 3;
    private static final int OVERFLOWING = 20;
    private static final int PERFECT = 30;
    private static final Card UNDERDOG = new Card(Suit.DIAMONDS, 2);

    public Progress {
        EnumSet<Achievement> copy = EnumSet.noneOf(Achievement.class);
        copy.addAll(earned);
        earned = Collections.unmodifiableSet(copy);
    }

    public static Progress start() {
        return new Progress(Optional.empty(), 0, 0, false, 0, 0, false, 0, Set.of());
    }

    public Progress after(Transition transition) {
        Card last = lastEquipped.orElse(null);
        int rising = risingEquips;
        int barehanded = barehandedKills;
        boolean aceBarehanded = aceSlainBarehanded;
        int acesWithWeapon = acesSlainWithWeapon;
        int avoided = avoids;
        boolean wasted = potionWasted;
        int cleanKills = cleanKillsInRoom;
        boolean underdog = false;
        Weapon broken = null;
        EnumSet<Achievement> now = EnumSet.noneOf(Achievement.class);
        now.addAll(earned);
        GameState state = transition.state();
        for (Event event : transition.events()) {
            if (event instanceof Event.CardDealt) {
                cleanKills = 0;
            } else if (event instanceof Event.RoomAvoided) {
                avoided++;
                cleanKills = 0;
            } else if (event instanceof Event.PotionWasted) {
                wasted = true;
            } else if (event instanceof Event.WeaponBroke broke) {
                broken = broke.weapon();
            } else if (event instanceof Event.WeaponEquipped equipped) {
                Card weapon = equipped.weapon();
                rising = last != null && weapon.value() == last.value() + 1 ? rising + 1 : 1;
                last = weapon;
                if (rising >= ARSENAL) {
                    now.add(Achievement.FULL_ARSENAL);
                }
            } else if (event instanceof Event.MonsterFought fought) {
                boolean ace = fought.monster().value() == Card.ACE;
                if (fought.usedWeapon()) {
                    acesWithWeapon += ace ? 1 : 0;
                    underdog |= ace && UNDERDOG.equals(last);
                    cleanKills += fought.damage() == 0 ? 1 : 0;
                    if (cleanKills >= FLAWLESS) {
                        now.add(Achievement.FLAWLESS);
                    }
                } else {
                    barehanded++;
                    aceBarehanded |= ace;
                }
            } else if (event instanceof Event.GameOver over && over.status() == Status.WON) {
                won(state, now, barehanded, aceBarehanded, avoided, wasted);
            }
        }
        if (underdog && state.status() != Status.LOST) {
            now.add(Achievement.UNDERDOG);
        }
        if (acesWithWeapon == 2) {
            now.add(Achievement.DRAGON_SLAYER);
        }
        // Brittle Blades can break a weapon on the kill that completes a pattern, so look at the broken one then.
        Optional<Weapon> weapon = broken != null ? Optional.of(broken) : state.weapon();
        weapon.ifPresent(stacked -> checkStack(stacked.slain(), now));
        return new Progress(
                Optional.ofNullable(last),
                rising,
                barehanded,
                aceBarehanded,
                acesWithWeapon,
                avoided,
                wasted,
                cleanKills,
                now);
    }

    /** The achievements earned since an earlier point in the same run, in their usual order. */
    public List<Achievement> earnedSince(Progress earlier) {
        List<Achievement> fresh = new ArrayList<>();
        for (Achievement achievement : earned) {
            if (!earlier.earned.contains(achievement)) {
                fresh.add(achievement);
            }
        }
        return fresh;
    }

    private static void won(
            GameState state, Set<Achievement> now, int barehanded, boolean aceBarehanded, int avoided, boolean wasted) {
        int plain = Scoring.plainScore(state);
        add(now, Achievement.BARE_KNUCKLES, barehanded >= BARE_KNUCKLES);
        add(now, Achievement.KNOCKOUT, aceBarehanded);
        add(now, Achievement.IRONMAN, avoided == 0);
        add(now, Achievement.ESCAPE_ARTIST, avoided >= ESCAPE_ARTIST);
        add(now, Achievement.WASTE_NOT, !wasted);
        add(now, Achievement.LAST_GASP, state.health() == 1);
        add(now, Achievement.OVERFLOWING, plain > OVERFLOWING);
        add(now, Achievement.PERFECT, plain == PERFECT);
        add(now, Achievement.MASOCHIST, state.rules().modifiers().size() >= MASOCHIST);
    }

    private static void checkStack(List<Card> stack, Set<Achievement> now) {
        int size = stack.size();
        add(
                now,
                Achievement.TWINS,
                size >= 2 && stack.get(size - 1).value() == stack.get(size - 2).value());
        boolean straight = endsInStraight(stack);
        add(now, Achievement.STRAIGHT, straight);
        add(now, Achievement.FLUSH, hasFlush(stack));
        add(
                now,
                Achievement.ROYAL_FLUSH,
                straight && stack.get(size - STACK).value() == Card.ACE && endsInOneSuit(stack));
    }

    private static void add(Set<Achievement> now, Achievement achievement, boolean earned) {
        if (earned) {
            now.add(achievement);
        }
    }

    private static boolean endsInStraight(List<Card> stack) {
        if (stack.size() < STACK) {
            return false;
        }
        for (int i = stack.size() - STACK + 1; i < stack.size(); i++) {
            if (stack.get(i).value() != stack.get(i - 1).value() - 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean endsInOneSuit(List<Card> stack) {
        Suit suit = stack.get(stack.size() - 1).suit();
        for (int i = stack.size() - STACK; i < stack.size(); i++) {
            if (stack.get(i).suit() != suit) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasFlush(List<Card> stack) {
        for (Suit suit : Suit.values()) {
            int count = 0;
            for (Card card : stack) {
                if (card.suit() == suit) {
                    count++;
                }
            }
            if (count >= STACK) {
                return true;
            }
        }
        return false;
    }
}
