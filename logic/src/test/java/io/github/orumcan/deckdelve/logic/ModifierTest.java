package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.card;
import static io.github.orumcan.deckdelve.logic.TestStates.cards;
import static io.github.orumcan.deckdelve.logic.TestStates.play;
import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Avoid;
import io.github.orumcan.deckdelve.logic.Action.Drink;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModifierTest {
    @Test
    void fragileStartsAtFifteen() {
        assertEquals(15, Rules.start(Seed.of(1), with(Modifier.FRAGILE)).state().health());
    }

    @Test
    void fragileHealsOnlyUpToFifteen() {
        GameState state = state().rules(with(Modifier.FRAGILE))
                .health(12)
                .room("7♥ 2♣ 3♣ 4♣")
                .dungeon("5♣")
                .build();
        Transition drink = Rules.apply(state, new Drink(0));

        assertEquals(15, drink.state().health());
        assertEquals(List.of(new Event.PotionDrunk(card("7♥"), 3)), drink.events());
    }

    @Test
    void fragileGivesThePotionBonusAtFifteen() {
        GameState state =
                state().rules(with(Modifier.FRAGILE)).health(15).room("6♥").build();

        assertEquals(21, Scoring.plainScore(Rules.apply(state, new Drink(0)).state()));
    }

    @Test
    void noRetreatNeverAllowsAvoiding() {
        GameState state = state().rules(with(Modifier.NO_RETREAT))
                .room("2♣ 3♣ 4♣ 5♣")
                .dungeon("6♣")
                .build();

        assertFalse(Rules.canAvoid(state));
        assertFalse(Rules.legalActions(state).contains(new Avoid()));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Avoid()));
    }

    @Test
    void wateredDownHealsOneLess() {
        GameState state = state().rules(with(Modifier.WATERED_DOWN))
                .health(10)
                .room("7♥ 2♣ 3♣ 4♣")
                .dungeon("5♣")
                .build();

        assertEquals(16, Rules.apply(state, new Drink(0)).state().health());
    }

    @Test
    void wateredDownTakesOneOffThePotionBonus() {
        GameState state = state().rules(with(Modifier.WATERED_DOWN)).room("9♥").build();

        assertEquals(28, Scoring.plainScore(Rules.apply(state, new Drink(0)).state()));
    }

    @Test
    void brittleBladesBreakAWeaponOnItsFourthKill() {
        GameState state = state().rules(with(Modifier.BRITTLE_BLADES))
                .weapon("9♦", "8♣ 7♠ 6♣")
                .room("5♣ 2♥ 3♥ 4♥")
                .dungeon("4♣")
                .build();
        Transition kill = Rules.apply(state, Fight.withWeapon(0));

        assertTrue(kill.state().weapon().isEmpty());
        assertEquals(cards("9♦ 8♣ 7♠ 6♣ 5♣"), kill.state().discard());
        Weapon broken = new Weapon(card("9♦"), cards("8♣ 7♠ 6♣ 5♣"));
        assertEquals(
                List.of(new Event.MonsterFought(card("5♣"), true, 0), new Event.WeaponBroke(broken)), kill.events());
    }

    @Test
    void brittleBladesKeepAWeaponUntilItsFourthKill() {
        GameState state = state().rules(with(Modifier.BRITTLE_BLADES))
                .weapon("9♦", "8♣ 7♠")
                .room("6♣ 2♥ 3♥ 4♥")
                .dungeon("5♣")
                .build();

        assertEquals(
                cards("8♣ 7♠ 6♣"),
                Rules.apply(state, Fight.withWeapon(0))
                        .state()
                        .weapon()
                        .orElseThrow()
                        .slain());
    }

    @Test
    void fogDealsTheNewestCardFaceDown() {
        Transition start = Rules.start(Seed.of(1), with(Modifier.FOG));

        assertEquals(OptionalInt.of(3), start.state().faceDown());
        List<Boolean> faceDown = start.events().stream()
                .map(event -> ((Event.CardDealt) event).faceDown())
                .toList();
        assertEquals(List.of(false, false, false, true), faceDown);
    }

    @Test
    void theFogCardCantBePickedBeforeItTurnsOver() {
        GameState state = fogRoom();

        assertTrue(Rules.legalActions(state).stream().noneMatch(action -> action.equals(new Drink(3))));
        assertThrows(IllegalActionException.class, () -> Rules.apply(state, new Drink(3)));
    }

    @Test
    void theFogCardTurnsOverOnceACardIsResolved() {
        Transition fight = Rules.apply(fogRoom(), Fight.barehanded(0));

        assertEquals(OptionalInt.empty(), fight.state().faceDown());
        assertTrue(fight.events().contains(new Event.CardRevealed(3, card("5♥"))));
        assertTrue(Rules.legalActions(fight.state()).contains(new Drink(3)));
    }

    @Test
    void aCarriedFogCardStaysFaceUpAndTheNewestNewCardIsHidden() {
        GameState next = play(fogRoom(), Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));

        assertEquals("[6♣, 7♣, 8♣, 5♥]", next.room().toString());
        assertEquals(OptionalInt.of(2), next.faceDown());
    }

    @Test
    void anAvoidedFogCardGoesToTheBottomUnseen() {
        GameState next = Rules.apply(fogRoom(), new Avoid()).state();

        assertEquals("[6♣, 7♣, 8♣, 9♣]", next.room().toString());
        assertEquals(cards("2♣ 3♣ 4♣ 5♥"), next.dungeon());
        assertEquals(OptionalInt.of(3), next.faceDown());
    }

    @Test
    void thornsMakeEveryWeaponFightCostAtLeastOne() {
        GameState state = state().rules(with(Modifier.THORNS))
                .weapon("9♦")
                .room("6♣ 2♥ 3♥ 4♥")
                .dungeon("5♣")
                .build();
        Transition kill = Rules.apply(state, Fight.withWeapon(0));

        assertEquals(19, kill.state().health());
        assertEquals(List.of(new Event.MonsterFought(card("6♣"), true, 1)), kill.events());
    }

    @Test
    void thornsLeaveDamageAboveOneAlone() {
        GameState state = state().rules(with(Modifier.THORNS))
                .weapon("5♦")
                .room("9♣ 2♥ 3♥ 4♥")
                .dungeon("5♣")
                .build();

        assertEquals(16, Rules.apply(state, Fight.withWeapon(0)).state().health());
    }

    @Test
    void fullClearDealsTheNextRoomOnlyOnceEveryCardIsTaken() {
        GameState state = state().rules(with(Modifier.FULL_CLEAR))
                .room("2♣ 3♣ 4♣ 5♥")
                .dungeon("6♣ 7♣ 8♣ 9♣ 10♣")
                .build();
        GameState three = play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2));
        assertEquals("[-, -, -, 5♥]", three.room().toString());

        GameState next = Rules.apply(three, new Drink(3)).state();
        assertEquals("[6♣, 7♣, 8♣, 9♣]", next.room().toString());
        assertTrue(Rules.canAvoid(next));
    }

    @Test
    void fullClearTakesTheLastCardEvenWhenTheVariantWouldLeaveIt() {
        RulesConfig rules = RulesConfig.OFFICIAL
                .withFinalRoom(RulesConfig.FinalRoom.LEAVE_LAST_CARD)
                .withModifiers(Set.of(Modifier.FULL_CLEAR));
        GameState state = state().rules(rules).room("2♣ 3♣ 4♣ 5♣").build();

        assertEquals(
                Status.PLAYING,
                play(state, Fight.barehanded(0), Fight.barehanded(1), Fight.barehanded(2))
                        .status());
    }

    @Test
    void hungerCostsOneHealthForEverySecondRoom() {
        assertEquals(20, Rules.start(Seed.of(1), with(Modifier.HUNGER)).state().health());

        Transition second = Rules.apply(play(hungry(1), new Drink(0), new Drink(1)), new Drink(2));
        assertEquals(11, second.state().health());
        assertTrue(second.events().contains(new Event.HungerFelt(1)));
        assertEquals("[6♣, 7♣, 8♣, 5♥]", second.state().room().toString());

        assertEquals(
                12, play(hungry(2), new Drink(0), new Drink(1), new Drink(2)).health());
    }

    @Test
    void hungerCostsHealthAfterAnAvoidToo() {
        GameState state = state().rules(with(Modifier.HUNGER))
                .room("2♣ 3♣ 4♣ 5♣")
                .dungeon("6♣")
                .build();

        assertEquals(19, Rules.apply(state, new Avoid()).state().health());
    }

    @Test
    void hungerCanEndTheRunBeforeTheNextRoomIsDealt() {
        GameState state = state().rules(with(Modifier.HUNGER))
                .health(1)
                .weapon("10♦")
                .room("4♣ 3♣ 2♣ 9♣")
                .dungeon("6♣")
                .build();
        GameState end = play(state, Fight.withWeapon(0), Fight.withWeapon(1), Fight.withWeapon(2));

        assertEquals(Status.LOST, end.status());
        assertEquals(0, end.health());
        assertEquals(cards("6♣"), end.dungeon());
    }

    @Test
    void modifiersMultiplyAWinRoundingDown() {
        Set<Modifier> modifiers = Set.of(Modifier.FRAGILE, Modifier.FOG);
        GameState end = Rules.apply(
                        state().rules(RulesConfig.OFFICIAL.withModifiers(modifiers))
                                .health(13)
                                .room("2♣")
                                .build(),
                        Fight.barehanded(0))
                .state();

        assertEquals(11, Scoring.plainScore(end));
        long product = (long) Modifier.FRAGILE.hundredths() * Modifier.FOG.hundredths();
        assertEquals(Math.floorDiv(11 * product, 100 * 100), Scoring.score(end));
    }

    @Test
    void everyModifierAtOnceStillMultipliesExactly() {
        Set<Modifier> all = EnumSet.allOf(Modifier.class);
        BigDecimal product = BigDecimal.ONE;
        for (Modifier modifier : all) {
            product = product.multiply(BigDecimal.valueOf(modifier.hundredths(), 2));
        }

        assertEquals(product.setScale(2, RoundingMode.HALF_UP).unscaledValue().intValue(), Scoring.multiplier(all));
        assertEquals(
                BigDecimal.valueOf(24)
                        .multiply(product)
                        .setScale(0, RoundingMode.FLOOR)
                        .intValue(),
                Scoring.multiplied(24, all));
    }

    @Test
    void aDeathKeepsItsPlainScore() {
        GameState death = Rules.apply(
                        state().rules(with(Modifier.FOG))
                                .health(5)
                                .room("Q♠ 2♣ 3♣ 4♣")
                                .dungeon("K♠")
                                .build(),
                        Fight.barehanded(0))
                .state();

        assertEquals(Scoring.plainScore(death), Scoring.score(death));
    }

    @Test
    void theMultiplierIsInHundredthsRoundedToTheNearest() {
        int fragile = Modifier.FRAGILE.hundredths();
        int fog = Modifier.FOG.hundredths();

        assertEquals(100, Scoring.multiplier(Set.of()));
        assertEquals(fragile, Scoring.multiplier(Set.of(Modifier.FRAGILE)));
        assertEquals((fragile * fog + 50) / 100, Scoring.multiplier(Set.of(Modifier.FRAGILE, Modifier.FOG)));
    }

    private static RulesConfig with(Modifier modifier) {
        return RulesConfig.OFFICIAL.withModifiers(Set.of(modifier));
    }

    /** A Room of potions at 10 health, with this many Rooms dealt so far. */
    private static GameState hungry(int rooms) {
        return state().rules(with(Modifier.HUNGER))
                .room("2♥ 3♥ 4♥ 5♥")
                .dungeon("6♣ 7♣ 8♣ 9♣")
                .health(10)
                .rooms(rooms)
                .build();
    }

    private static GameState fogRoom() {
        return state().rules(with(Modifier.FOG))
                .room("2♣ 3♣ 4♣ 5♥")
                .faceDown(3)
                .dungeon("6♣ 7♣ 8♣ 9♣")
                .build();
    }
}
