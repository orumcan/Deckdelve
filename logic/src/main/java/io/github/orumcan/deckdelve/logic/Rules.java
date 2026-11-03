package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Rules {
    public static final int MAX_HEALTH = 20;

    private Rules() {}

    public static Transition start(Seed seed, RulesConfig rules) {
        Draft draft = new Draft(seed, rules, Deck.shuffled(seed));
        draft.dealRoom();
        return draft.finish();
    }

    /** Applies an action, or throws {@link IllegalActionException} if it isn't allowed right now. */
    public static Transition apply(GameState state, Action action) {
        if (state.isOver()) {
            throw new IllegalActionException("The run is over.");
        }
        Draft draft = new Draft(state);
        if (action instanceof Action.Avoid) {
            avoid(state, draft);
        } else if (action instanceof Action.Equip equip) {
            equip(draft, cardIn(state, equip.slot(), CardType.WEAPON), equip.slot());
        } else if (action instanceof Action.Drink drink) {
            drink(draft, cardIn(state, drink.slot(), CardType.POTION), drink.slot());
        } else if (action instanceof Action.Fight fight) {
            fight(state, draft, cardIn(state, fight.slot(), CardType.MONSTER), fight);
        }
        if (draft.status == Status.PLAYING) {
            advance(draft);
        }
        return draft.finish();
    }

    public static List<Action> legalActions(GameState state) {
        if (state.isOver()) {
            return List.of();
        }
        List<Action> actions = new ArrayList<>();
        if (canAvoid(state)) {
            actions.add(new Action.Avoid());
        }
        for (int slot = 0; slot < Room.SIZE; slot++) {
            Optional<Card> card = state.room().card(slot);
            if (card.isEmpty()) {
                continue;
            }
            switch (card.get().type()) {
                case WEAPON -> actions.add(new Action.Equip(slot));
                case POTION -> actions.add(new Action.Drink(slot));
                case MONSTER -> {
                    actions.add(Action.Fight.barehanded(slot));
                    if (canUseWeapon(state, card.get())) {
                        actions.add(Action.Fight.withWeapon(slot));
                    }
                }
            }
        }
        return List.copyOf(actions);
    }

    public static boolean canAvoid(GameState state) {
        return !state.isOver()
                && !state.lastRoomAvoided()
                && state.resolvedInRoom() == 0
                && !state.dungeon().isEmpty();
    }

    public static boolean canUseWeapon(GameState state, Card monster) {
        return state.weapon()
                .map(weapon -> weapon.canFight(monster, state.rules().weaponRule()))
                .orElse(false);
    }

    private static void avoid(GameState state, Draft draft) {
        if (state.lastRoomAvoided()) {
            throw new IllegalActionException("The previous Room was avoided, so this one has to be faced.");
        }
        if (state.resolvedInRoom() > 0) {
            throw new IllegalActionException("A Room can't be avoided once a card in it is resolved.");
        }
        if (state.dungeon().isEmpty()) {
            throw new IllegalActionException("The last Room can't be avoided.");
        }
        List<Card> cards = state.room().cards();
        draft.dungeon.addAll(cards);
        Arrays.fill(draft.room, null);
        draft.lastRoomAvoided = true;
        draft.emit(new Event.RoomAvoided(cards));
    }

    private static void equip(Draft draft, Card weapon, int slot) {
        draft.resolve(slot);
        Weapon replaced = draft.weapon;
        if (replaced != null) {
            draft.discard.add(replaced.card());
            draft.discard.addAll(replaced.slain());
        }
        draft.weapon = Weapon.of(weapon);
        draft.emit(new Event.WeaponEquipped(weapon, Optional.ofNullable(replaced)));
    }

    private static void drink(Draft draft, Card potion, int slot) {
        draft.resolve(slot);
        draft.discard.add(potion);
        if (draft.potionUsed) {
            draft.emit(new Event.PotionWasted(potion));
            return;
        }
        int healed = Math.min(potion.value(), MAX_HEALTH - draft.health);
        draft.health += healed;
        draft.potionUsed = true;
        draft.emit(new Event.PotionDrunk(potion, healed));
    }

    private static void fight(GameState state, Draft draft, Card monster, Action.Fight fight) {
        int damage;
        if (fight.useWeapon()) {
            Weapon weapon = state.weapon().orElseThrow(() -> new IllegalActionException("No weapon is equipped."));
            if (!weapon.canFight(monster, state.rules().weaponRule())) {
                throw new IllegalActionException(weapon.card() + " last slew "
                        + weapon.lastSlain().orElseThrow() + ", so it can't fight " + monster + ".");
            }
            damage = weapon.damageAgainst(monster);
            draft.weapon = weapon.slay(monster);
        } else {
            damage = monster.value();
            draft.discard.add(monster);
        }
        draft.resolve(fight.slot());
        draft.health -= damage;
        draft.emit(new Event.MonsterFought(monster, fight.useWeapon(), damage));
        if (draft.health <= 0) {
            draft.status = Status.LOST;
        }
    }

    private static void advance(Draft draft) {
        int left = draft.roomCount();
        boolean dungeonEmpty = draft.dungeon.isEmpty();
        if (left == 0) {
            // Either the Room was just avoided, or the last Room is finished.
            if (dungeonEmpty) {
                draft.status = Status.WON;
            } else {
                draft.dealRoom();
            }
        } else if (left == 1 && draft.resolvedInRoom > 0) {
            if (!dungeonEmpty) {
                draft.lastRoomAvoided = false;
                draft.dealRoom();
            } else if (draft.rules.finalRoom() == RulesConfig.FinalRoom.LEAVE_LAST_CARD) {
                draft.status = Status.WON;
            }
        }
    }

    private static Card cardIn(GameState state, int slot, CardType type) {
        if (slot < 0 || slot >= Room.SIZE) {
            throw new IllegalActionException("Slots run from 0 to " + (Room.SIZE - 1) + ", not " + slot + ".");
        }
        Card card =
                state.room().card(slot).orElseThrow(() -> new IllegalActionException("Slot " + slot + " is empty."));
        if (card.type() != type) {
            throw new IllegalActionException("Slot " + slot + " holds " + card + ", which isn't a " + type + ".");
        }
        return card;
    }
}
