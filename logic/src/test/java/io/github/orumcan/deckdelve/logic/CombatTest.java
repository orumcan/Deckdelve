package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.card;
import static io.github.orumcan.deckdelve.logic.TestStates.cards;
import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Fight;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CombatTest {
    @Test
    void barehandedCostsTheMonstersFullValue() {
        GameState state = state().room("Q♠ 2♣ 3♣ 4♣").dungeon("5♣").build();
        Transition fight = Rules.apply(state, Fight.barehanded(0));

        assertEquals(8, fight.state().health());
        assertEquals(cards("Q♠"), fight.state().discard());
        assertEquals(List.of(new Event.MonsterFought(card("Q♠"), false, 12)), fight.events());
    }

    @Test
    void aWeaponTakesItsValueOffTheDamageAndKeepsTheMonster() {
        GameState state = state().weapon("5♦").room("J♣ 2♣ 3♣ 4♣").dungeon("5♣").build();
        Transition fight = Rules.apply(state, Fight.withWeapon(0));

        assertEquals(14, fight.state().health());
        assertEquals(cards("J♣"), fight.state().weapon().orElseThrow().slain());
        assertEquals(List.of(), fight.state().discard());
        assertEquals(List.of(new Event.MonsterFought(card("J♣"), true, 6)), fight.events());
    }

    @Test
    void aStrongerWeaponTakesNoDamage() {
        GameState state = state().weapon("9♦").room("3♣ 2♣ 4♣ 5♣").dungeon("6♣").build();
        assertEquals(20, Rules.apply(state, Fight.withWeapon(0)).state().health());
    }

    @Test
    void aUsedWeaponFightsMonstersOfEqualOrLowerValue() {
        GameState state =
                state().weapon("5♦", "6♣").room("6♠ 5♣ 7♣ 2♣").dungeon("8♣").build();

        assertTrue(Rules.canUseWeapon(state, card("6♠")));
        assertTrue(Rules.canUseWeapon(state, card("5♣")));
        assertFalse(Rules.canUseWeapon(state, card("7♣")));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, Fight.withWeapon(2)));
    }

    @Test
    void theStrictVariantRulesOutTies() {
        RulesConfig strict = RulesConfig.OFFICIAL.withWeaponRule(WeaponRule.STRICTLY_LOWER);
        GameState state = state().rules(strict)
                .weapon("5♦", "6♣")
                .room("6♠ 5♣ 7♣ 2♣")
                .dungeon("8♣")
                .build();

        assertFalse(Rules.canUseWeapon(state, card("6♠")));
        assertTrue(Rules.canUseWeapon(state, card("5♣")));
    }

    /** The rulebook's example: a 5 that slew a queen can fight a 6, but then not a queen, and it stays equipped. */
    @Test
    void followsTheRulebookExample() {
        GameState state =
                state().weapon("5♦", "Q♠").room("6♣ Q♣ 2♣ 3♣").dungeon("4♣").build();

        GameState afterSix = Rules.apply(state, Fight.withWeapon(0)).state();
        assertEquals(19, afterSix.health());
        assertFalse(Rules.canUseWeapon(afterSix, card("Q♣")));

        GameState afterQueen = Rules.apply(afterSix, Fight.barehanded(1)).state();
        assertEquals(7, afterQueen.health());
        assertEquals(card("5♦"), afterQueen.weapon().orElseThrow().card());
    }

    @Test
    void fightingBarehandedIsAlwaysAllowed() {
        GameState state = state().weapon("9♦").room("8♣ 2♣ 3♣ 4♣").dungeon("5♣").build();
        assertEquals(12, Rules.apply(state, Fight.barehanded(0)).state().health());
    }

    @Test
    void aNewWeaponReplacesTheOldOneAndDiscardsItsMonsters() {
        GameState state =
                state().weapon("5♦", "Q♠ 6♣").room("8♦ 2♣ 3♣ 4♣").dungeon("5♣").build();
        Transition equip = Rules.apply(state, new Action.Equip(0));

        assertEquals(Optional.of(Weapon.of(card("8♦"))), equip.state().weapon());
        assertEquals(cards("5♦ Q♠ 6♣"), equip.state().discard());
        assertEquals(List.of(new Event.WeaponEquipped(card("8♦"), state.weapon())), equip.events());
    }

    @Test
    void aWeaponFightNeedsAWeapon() {
        GameState state = state().room("8♣ 2♣ 3♣ 4♣").dungeon("5♣").build();
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, Fight.withWeapon(0)));
    }
}
