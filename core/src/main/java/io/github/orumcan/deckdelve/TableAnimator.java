package io.github.orumcan.deckdelve;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Event;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Weapon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plays an action's events over a table already laid out with their result. Cards fly from where they were to their
 * new places, which stay empty until they land; the piles count cards as they arrive and leave; and the health bar runs
 * to its new value with the sum in its label.
 */
final class TableAnimator {
    // Seconds at normal speed.
    private static final float MOVE = 0.3f;
    private static final float TURN = 0.2f;
    private static final float STAGGER = 0.08f;
    private static final float HEALTH = 0.35f;
    /** How long a health change's sum stays in the bar's label once the bar has moved. */
    private static final float SUM = 0.8f;
    /** Lets the last blow of a run sink in before the end-of-run panel. */
    private static final float PAUSE = 0.35f;

    /** The table before the action: its state, where each card sat, and where the weapon's next kill would go. */
    record Before(GameState state, Map<Card, Rectangle> cards, Optional<Rectangle> nextKill) {}

    private final Ui ui;
    private final Sounds sounds;
    private final Group layer;
    private final Before before;
    private final Map<Card, CardActor> placed;
    private final Pile dungeon;
    private final Pile discard;
    private final HealthBar health;
    private final float move;
    private final float turn;
    private final float stagger;
    private final float healthTime;
    private final float sumTime;
    private final float pause;

    private final Set<Card> moved = new HashSet<>();
    /** The health after every change scheduled so far. */
    private int healthNow;
    /** When the health bar is free for its next change. */
    private float healthFree;
    /** When the last card lands. */
    private float end;

    TableAnimator(
            Ui ui,
            Sounds sounds,
            Group layer,
            AnimationSpeed speed,
            Before before,
            Map<Card, CardActor> placed,
            Pile dungeon,
            Pile discard,
            HealthBar health) {
        this.ui = ui;
        this.sounds = sounds;
        this.layer = layer;
        this.before = before;
        this.placed = placed;
        this.dungeon = dungeon;
        this.discard = discard;
        this.health = health;
        move = MOVE * speed.scale;
        turn = TURN * speed.scale;
        stagger = STAGGER * speed.scale;
        healthTime = HEALTH * speed.scale;
        sumTime = SUM * speed.scale;
        pause = PAUSE * speed.scale;
    }

    static Rectangle bounds(Actor actor) {
        Vector2 corner = actor.localToStageCoordinates(new Vector2());
        return new Rectangle(corner.x, corner.y, actor.getWidth(), actor.getHeight());
    }

    /**
     * Schedules the action's cards first, then a Fog card turning over, Hunger, and the next room's deal, each after
     * the one before. Calls done once everything has landed, after a pause if the run is over.
     */
    void play(List<Event> events, Runnable done) {
        GameState old = before.state();
        dungeon.count(old.dungeon().size());
        discard.count(old.discard().size());
        healthNow = old.health();
        health.showFrom(healthNow);

        Optional<Weapon> broken = Optional.empty();
        boolean over = false;
        for (Event event : events) {
            if (event instanceof Event.WeaponBroke broke) {
                broken = Optional.of(broke.weapon());
            } else if (event instanceof Event.GameOver) {
                over = true;
            }
        }
        for (Event event : events) {
            if (event instanceof Event.RoomAvoided avoided) {
                cue(event, 0);
                for (int i = 0; i < avoided.cards().size(); i++) {
                    toPile(avoided.cards().get(i), dungeon, i * stagger);
                }
            } else if (event instanceof Event.WeaponEquipped equipped) {
                float drawn = equipped.replaced().isPresent() ? 2 * stagger : 0;
                equipped.replaced().ifPresent(replaced -> toDiscard(replaced, 0));
                arrive(equipped.weapon(), drawn);
                cue(event, drawn);
            } else if (event instanceof Event.PotionDrunk drunk) {
                cue(event, 0);
                toPile(drunk.potion(), discard, 0);
                changeHealth(drunk.healed(), 0);
            } else if (event instanceof Event.PotionWasted wasted) {
                cue(event, 0);
                toPile(wasted.potion(), discard, 0);
            } else if (event instanceof Event.MonsterFought fought) {
                if (!fought.usedWeapon()) {
                    toPile(fought.monster(), discard, 0);
                } else if (broken.isPresent()) {
                    breakWeapon(broken.get(), fought.monster());
                } else {
                    arrive(fought.monster(), 0);
                }
                // The blow lands halfway, as the health starts to drop.
                cue(event, move / 2);
                changeHealth(-fought.damage(), move / 2);
            }
        }
        float time = end;
        for (Event event : events) {
            if (event instanceof Event.CardRevealed revealed) {
                cue(event, time);
                time = reveal(revealed.card(), time);
            }
        }
        for (Event event : events) {
            if (event instanceof Event.HungerFelt hunger) {
                changeHealth(-hunger.damage(), time);
                time = healthFree;
            }
        }
        int dealt = 0;
        for (Event event : events) {
            if (event instanceof Event.CardDealt card) {
                float start = time + dealt++ * stagger;
                cue(event, start);
                deal(card.card(), card.faceDown(), start);
            }
        }
        slideTheRest();
        float finished = Math.max(end, healthFree) + (over ? pause : 0);
        for (Event event : events) {
            if (event instanceof Event.GameOver) {
                cue(event, finished);
            }
        }
        layer.addAction(Actions.delay(finished, Actions.run(done)));
    }

    /** Plays the event's sound at a time, if it has one. */
    private void cue(Event event, float at) {
        Sounds.Effect.of(event)
                .ifPresent(effect -> layer.addAction(Actions.delay(at, Actions.run(() -> sounds.play(effect)))));
    }

    /** A card moving within the table, such as a weapon being equipped; its new place stays empty until it lands. */
    private void arrive(Card card, float start) {
        CardActor target = placed.get(card);
        Rectangle from = before.cards().get(card);
        moved.add(card);
        if (target == null || from == null) {
            return;
        }
        FlyingCard flying = fly(card, !wasFaceDown(card), from);
        target.setVisible(false);
        flying.addAction(Actions.sequence(
                Actions.delay(start),
                travel(bounds(target)),
                Actions.run(() -> target.setVisible(true)),
                Actions.removeActor()));
        end = Math.max(end, start + move);
    }

    /** A card turning face down onto a pile, which counts it as it lands. */
    private void toPile(Card card, Pile pile, float start) {
        Rectangle from = before.cards().get(card);
        moved.add(card);
        if (from == null) {
            layer.addAction(Actions.delay(start + move, Actions.run(pile::addCard)));
            end = Math.max(end, start + move);
            return;
        }
        boolean faceUp = !wasFaceDown(card);
        FlyingCard flying = fly(card, faceUp, from);
        flying.addAction(Actions.sequence(
                Actions.delay(start),
                Actions.parallel(travel(pile.topCard()), faceUp ? flying.turnOver(turn) : Actions.delay(0)),
                Actions.run(pile::addCard),
                Actions.removeActor()));
        end = Math.max(end, start + move);
    }

    /** A weapon and its kills going to the discard, the weapon first, as the engine discards them. */
    private void toDiscard(Weapon weapon, float start) {
        List<Card> cards = new ArrayList<>();
        cards.add(weapon.card());
        cards.addAll(weapon.slain());
        for (int i = 0; i < cards.size(); i++) {
            toPile(cards.get(i), discard, start + i * stagger / 2);
        }
    }

    /**
     * Brittle Blades: the killing blow lands on the stack, and after a beat the weapon goes to the discard with every
     * kill on it. The new layout has no weapon any more, so the weapon and its older kills wait where they were.
     */
    private void breakWeapon(Weapon weapon, Card killer) {
        float breaks = move + 2 * stagger;
        layer.addAction(Actions.delay(breaks, Actions.run(() -> sounds.play(Sounds.Effect.BREAK))));
        List<Card> cards = new ArrayList<>();
        cards.add(weapon.card());
        cards.addAll(weapon.slain());
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (!card.equals(killer)) {
                toPile(card, discard, breaks + i * stagger / 2);
                continue;
            }
            Rectangle from = before.cards().get(card);
            Optional<Rectangle> stacked = before.nextKill();
            moved.add(card);
            if (from == null || stacked.isEmpty()) {
                layer.addAction(Actions.delay(breaks + move, Actions.run(discard::addCard)));
                continue;
            }
            FlyingCard flying = fly(card, true, from);
            flying.addAction(Actions.sequence(
                    travel(stacked.get()),
                    Actions.delay(breaks + i * stagger / 2 - move),
                    Actions.parallel(travel(discard.topCard()), flying.turnOver(turn)),
                    Actions.run(discard::addCard),
                    Actions.removeActor()));
        }
        end = Math.max(end, breaks + cards.size() * stagger / 2 + move);
    }

    /** Fog's face-down card turning over where it lies. */
    private float reveal(Card card, float start) {
        CardActor target = placed.get(card);
        moved.add(card);
        if (target == null) {
            return start;
        }
        FlyingCard flying = fly(card, false, bounds(target));
        target.setVisible(false);
        flying.addAction(Actions.sequence(
                Actions.delay(start),
                flying.turnOver(turn),
                Actions.run(() -> target.setVisible(true)),
                Actions.removeActor()));
        end = Math.max(end, start + turn);
        return start + turn;
    }

    /** A card dealt from the Dungeon pile to its place, turning face up as it arrives unless Fog keeps it down. */
    private void deal(Card card, boolean faceDown, float start) {
        CardActor target = placed.get(card);
        moved.add(card);
        layer.addAction(Actions.delay(start, Actions.run(dungeon::takeCard)));
        if (target == null) {
            return;
        }
        FlyingCard flying = fly(card, false, dungeon.topCard());
        flying.setVisible(false);
        target.setVisible(false);
        flying.addAction(Actions.sequence(
                Actions.delay(start),
                Actions.visible(true),
                Actions.parallel(
                        travel(bounds(target)),
                        faceDown ? Actions.delay(0) : Actions.delay(move - turn, flying.turnOver(turn))),
                Actions.run(() -> target.setVisible(true)),
                Actions.removeActor()));
        end = Math.max(end, start + move);
    }

    /**
     * Any other card whose place changed slides there: the compact layout's card left from the last room, or cards
     * nudged by a banner appearing above them.
     */
    private void slideTheRest() {
        for (Map.Entry<Card, CardActor> entry : placed.entrySet()) {
            Card card = entry.getKey();
            Rectangle from = before.cards().get(card);
            if (moved.contains(card) || from == null || bounds(entry.getValue()).equals(from)) {
                continue;
            }
            arrive(card, 0);
        }
    }

    private void changeHealth(int change, float at) {
        if (change == 0) {
            return;
        }
        float start = Math.max(at, healthFree);
        int from = healthNow;
        healthNow += change;
        health.change(from, healthNow, start, healthTime, sumTime);
        healthFree = start + healthTime;
    }

    private FlyingCard fly(Card card, boolean faceUp, Rectangle from) {
        FlyingCard flying = new FlyingCard(ui.cards, card, faceUp, from);
        layer.addActor(flying);
        return flying;
    }

    private Action travel(Rectangle to) {
        return Actions.parallel(
                Actions.moveTo(to.x, to.y, move, Interpolation.pow2Out),
                Actions.sizeTo(to.width, to.height, move, Interpolation.pow2Out));
    }

    private boolean wasFaceDown(Card card) {
        GameState old = before.state();
        return old.faceDown().isPresent()
                && old.room()
                        .card(old.faceDown().getAsInt())
                        .filter(card::equals)
                        .isPresent();
    }
}
