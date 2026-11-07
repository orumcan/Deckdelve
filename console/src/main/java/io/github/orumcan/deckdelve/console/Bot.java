package io.github.orumcan.deckdelve.console;

import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.Room;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Status;
import io.github.orumcan.deckdelve.logic.Weapon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple player for measuring how much each modifier lowers the win rate. It tries every order of the cards it has
 * to resolve in the current Room, scores where each order leaves it, and avoids a Room only when every order is
 * deadly. It never looks at a face-down card or at the cards a simulated move deals.
 */
final class Bot {
    private static final double WON = 1000;
    private static final double LOST = -1000;

    // Tuned on the official rules, where the bot wins about 10% of seeds.
    private static final double WEAPON = 0.3;
    private static final int WEAPON_USES = 3;
    private static final double CARRIED_MONSTER = 0.8;
    private static final double CARRIED_POTION = 0.6;
    private static final double CARRIED_WEAPON = 0.3;

    private Bot() {}

    static Action choose(GameState state) {
        Action best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Action action : Rules.legalActions(state)) {
            if (action instanceof Action.Avoid) {
                continue;
            }
            double value = explore(state, state, action, visible(state), needed(state));
            if (value > bestValue) {
                bestValue = value;
                best = action;
            }
        }
        if (Rules.canAvoid(state) && (best == null || bestValue < LOST / 2)) {
            return new Action.Avoid();
        }
        return best;
    }

    private static List<Integer> visible(GameState state) {
        List<Integer> visible = new ArrayList<>();
        for (int slot = 0; slot < Room.SIZE; slot++) {
            if (state.room().card(slot).isPresent() && !state.isFaceDown(slot)) {
                visible.add(slot);
            }
        }
        return visible;
    }

    /** How many more cards the plan resolves: all but one, or every card in the last Room or under Full Clear. */
    private static int needed(GameState state) {
        int cards = state.room().count();
        boolean all = state.dungeon().isEmpty() || state.rules().has(Modifier.FULL_CLEAR);
        return Math.min(visible(state).size(), all ? cards : cards - 1);
    }

    private static double explore(GameState root, GameState state, Action action, List<Integer> open, int needed) {
        GameState next = Rules.apply(state, action).state();
        List<Integer> rest = new ArrayList<>(open);
        rest.remove(Integer.valueOf(slotOf(action)));
        if (next.isOver() || needed == 1 || rest.isEmpty()) {
            return value(root, next, leftover(root, rest));
        }
        double best = Double.NEGATIVE_INFINITY;
        for (Action following : Rules.legalActions(next)) {
            if (!(following instanceof Action.Avoid) && rest.contains(slotOf(following))) {
                best = Math.max(best, explore(root, next, following, rest, needed - 1));
            }
        }
        return best == Double.NEGATIVE_INFINITY ? value(root, next, leftover(root, rest)) : best;
    }

    /** The one card a plan leaves in the Room, if the plan knows it. */
    private static Card leftover(GameState root, List<Integer> unresolved) {
        return unresolved.size() == 1 && root.faceDown().isEmpty()
                ? root.room().card(unresolved.get(0)).orElseThrow()
                : null;
    }

    /**
     * How good a position is: health, plus the damage the weapon can still save on the monsters ahead, plus what the
     * card left over is worth. The monsters ahead are the ones in the Dungeon when the plan started, so a plan never
     * learns about the cards it deals.
     */
    private static double value(GameState root, GameState state, Card leftover) {
        if (state.status() == Status.WON) {
            return WON + state.health();
        }
        if (state.status() == Status.LOST) {
            return LOST + state.health();
        }
        double value = state.health();
        if (state.weapon().isPresent()) {
            List<Card> ahead = new ArrayList<>(root.dungeon());
            if (leftover != null) {
                ahead.add(leftover);
            }
            value += WEAPON * savings(state.weapon().get(), state.rules(), ahead);
        }
        if (leftover != null) {
            value += switch (leftover.type()) {
                case MONSTER -> -CARRIED_MONSTER * damage(state, leftover);
                case POTION -> CARRIED_POTION * Rules.healing(leftover, state.rules());
                case WEAPON -> CARRIED_WEAPON * leftover.value();
            };
        }
        return value;
    }

    /** What a monster would cost with the weapon as it stands: the difference if it can fight, all of it if not. */
    private static int damage(GameState state, Card monster) {
        return Rules.canUseWeapon(state, monster)
                ? Rules.weaponDamage(state.weapon().orElseThrow(), monster, state.rules())
                : monster.value();
    }

    /** The damage the weapon could save on its best few fights against the monsters ahead. */
    private static double savings(Weapon weapon, RulesConfig rules, List<Card> ahead) {
        int reach = weapon.lastSlain().map(Card::value).orElse(Card.ACE);
        if (rules.weaponRule() == RulesConfig.WeaponRule.STRICTLY_LOWER
                && weapon.lastSlain().isPresent()) {
            reach--;
        }
        int uses = rules.has(Modifier.BRITTLE_BLADES)
                ? Rules.BRITTLE_KILLS - weapon.slain().size()
                : WEAPON_USES;
        List<Integer> saved = new ArrayList<>();
        for (Card card : ahead) {
            if (card.type() == CardType.MONSTER && card.value() <= reach) {
                saved.add(card.value() - Rules.weaponDamage(weapon, card, rules));
            }
        }
        saved.sort(Collections.reverseOrder());
        double total = 0;
        for (int i = 0; i < Math.min(uses, saved.size()); i++) {
            total += saved.get(i);
        }
        return total;
    }

    private static int slotOf(Action action) {
        if (action instanceof Action.Equip equip) {
            return equip.slot();
        }
        if (action instanceof Action.Drink drink) {
            return drink.slot();
        }
        if (action instanceof Action.Fight fight) {
            return fight.slot();
        }
        return -1;
    }
}
