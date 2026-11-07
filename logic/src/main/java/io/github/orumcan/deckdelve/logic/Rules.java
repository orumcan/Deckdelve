package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Rules {
    /** The kill that breaks a weapon under Brittle Blades. */
    public static final int BRITTLE_KILLS = 4;

    private static final int HEALTH = 20;
    private static final int FRAGILE_HEALTH = 15;
    private static final int HUNGER_COST = 1;

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
            draft.revealFaceDown();
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
            if (card.isEmpty() || state.isFaceDown(slot)) {
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
                && !state.rules().has(Modifier.NO_RETREAT)
                && !state.lastRoomAvoided()
                && state.resolvedInRoom() == 0
                && !state.dungeon().isEmpty();
    }

    public static boolean canUseWeapon(GameState state, Card monster) {
        return state.weapon()
                .map(weapon -> weapon.canFight(monster, state.rules().weaponRule()))
                .orElse(false);
    }

    /** The health a run starts with, which is also the most it can heal to: 20, or 15 under Fragile. */
    public static int maxHealth(RulesConfig rules) {
        return rules.has(Modifier.FRAGILE) ? FRAGILE_HEALTH : HEALTH;
    }

    /** What a monster costs when fought with the weapon: whatever it has over the weapon, and at least 1 under Thorns. */
    public static int weaponDamage(Weapon weapon, Card monster, RulesConfig rules) {
        int damage = weapon.damageAgainst(monster);
        return rules.has(Modifier.THORNS) ? Math.max(1, damage) : damage;
    }

    /** The most a potion heals: its value, or 1 less under Watered Down. */
    public static int healing(Card potion, RulesConfig rules) {
        return rules.has(Modifier.WATERED_DOWN) ? potion.value() - 1 : potion.value();
    }

    private static void avoid(GameState state, Draft draft) {
        if (state.rules().has(Modifier.NO_RETREAT)) {
            throw new IllegalActionException("No Retreat is on, so Rooms can't be avoided.");
        }
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
        draft.clearFaceDown();
        draft.lastRoomAvoided = true;
        draft.emit(new Event.RoomAvoided(cards));
    }

    private static void equip(Draft draft, Card weapon, int slot) {
        draft.resolve(slot);
        Weapon replaced = draft.weapon;
        if (replaced != null) {
            draft.discard(replaced);
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
        int healed = Math.min(healing(potion, draft.rules), maxHealth(draft.rules) - draft.health);
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
            damage = weaponDamage(weapon, monster, state.rules());
            draft.weapon = weapon.slay(monster);
        } else {
            damage = monster.value();
            draft.discard.add(monster);
        }
        draft.resolve(fight.slot());
        draft.health -= damage;
        draft.emit(new Event.MonsterFought(monster, fight.useWeapon(), damage));
        if (fight.useWeapon()
                && state.rules().has(Modifier.BRITTLE_BLADES)
                && draft.weapon.slain().size() == BRITTLE_KILLS) {
            draft.discard(draft.weapon);
            draft.emit(new Event.WeaponBroke(draft.weapon));
            draft.weapon = null;
        }
        if (draft.health <= 0) {
            draft.status = Status.LOST;
        }
    }

    private static void advance(Draft draft) {
        int left = draft.roomCount();
        boolean dungeonEmpty = draft.dungeon.isEmpty();
        if (left == 0) {
            // The Room was just avoided, cleared under Full Clear, or it was the last one.
            if (dungeonEmpty) {
                draft.status = Status.WON;
            } else {
                if (draft.resolvedInRoom > 0) {
                    draft.lastRoomAvoided = false;
                }
                nextRoom(draft);
            }
        } else if (left == 1 && draft.resolvedInRoom > 0 && !draft.rules.has(Modifier.FULL_CLEAR)) {
            if (!dungeonEmpty) {
                draft.lastRoomAvoided = false;
                nextRoom(draft);
            } else if (draft.rules.finalRoom() == RulesConfig.FinalRoom.LEAVE_LAST_CARD) {
                draft.status = Status.WON;
            }
        }
    }

    /** Deals the next Room. Under Hunger, every second Room costs health first, and that can end the run instead. */
    private static void nextRoom(Draft draft) {
        if (draft.rules.has(Modifier.HUNGER) && (draft.rooms + 1) % 2 == 0) {
            draft.health -= HUNGER_COST;
            draft.emit(new Event.HungerFelt(HUNGER_COST));
            if (draft.health <= 0) {
                draft.status = Status.LOST;
                return;
            }
        }
        draft.dealRoom();
    }

    private static Card cardIn(GameState state, int slot, CardType type) {
        if (slot < 0 || slot >= Room.SIZE) {
            throw new IllegalActionException("Slots run from 0 to " + (Room.SIZE - 1) + ", not " + slot + ".");
        }
        Card card =
                state.room().card(slot).orElseThrow(() -> new IllegalActionException("Slot " + slot + " is empty."));
        if (state.isFaceDown(slot)) {
            throw new IllegalActionException(
                    "Slot " + slot + " is face down until another card in the Room is resolved.");
        }
        if (card.type() != type) {
            throw new IllegalActionException("Slot " + slot + " holds " + card + ", which isn't a " + type + ".");
        }
        return card;
    }
}
