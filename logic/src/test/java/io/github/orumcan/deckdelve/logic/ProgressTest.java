package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Drink;
import io.github.orumcan.deckdelve.logic.Action.Equip;
import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgressTest {
    @Test
    void fullArsenalNeedsThreeRisingEquipsInARow() {
        GameState state = state().room("5♦ 2♣ 6♦ 7♦").dungeon("3♣ 4♣ 8♣ 9♣").build();

        Set<Achievement> earned = replay(state, new Equip(0), Fight.barehanded(1), new Equip(2), new Equip(3))
                .earned();
        assertTrue(earned.contains(Achievement.FULL_ARSENAL));
    }

    @Test
    void fullArsenalNeedsTheEquipsInRisingOrder() {
        GameState state = state().room("5♦ 7♦ 6♦ 2♣").dungeon("3♣ 4♣ 8♣ 9♣").build();

        assertFalse(
                replay(state, new Equip(0), new Equip(1), new Equip(2)).earned().contains(Achievement.FULL_ARSENAL));
    }

    @Test
    void straightNeedsFiveKillsEachOneLower() {
        GameState straight =
                state().weapon("10♦", "8♣ 7♠ 6♣ 5♠").room("4♣ 2♥ 3♥ 9♥").build();
        GameState gaps =
                state().weapon("10♦", "9♣ 7♠ 5♣ 3♠").room("2♣ 2♥ 3♥ 9♥").build();

        assertTrue(replay(straight, Fight.withWeapon(0)).earned().contains(Achievement.STRAIGHT));
        assertFalse(replay(gaps, Fight.withWeapon(0)).earned().contains(Achievement.STRAIGHT));
    }

    @Test
    void flushNeedsFiveOfOneSuitOnOneWeapon() {
        GameState flush =
                state().weapon("10♦", "K♣ 9♣ 7♣ 4♣").room("2♣ 2♥ 3♥ 9♥").build();
        GameState mixed =
                state().weapon("10♦", "K♣ 9♠ 7♣ 4♣").room("2♣ 2♥ 3♥ 9♥").build();

        assertTrue(replay(flush, Fight.withWeapon(0)).earned().contains(Achievement.FLUSH));
        assertFalse(replay(mixed, Fight.withWeapon(0)).earned().contains(Achievement.FLUSH));
    }

    @Test
    void dragonSlayerNeedsBothAcesSlainWithAWeapon() {
        GameState state = state().weapon("10♦").room("A♠ 2♥ 3♥ 9♥").build();
        Progress oneAce = new Progress(Optional.empty(), 0, 0, false, 1, 0, false, 0, Set.of());

        assertTrue(after(oneAce, state, Fight.withWeapon(0)).earned().contains(Achievement.DRAGON_SLAYER));
        assertFalse(replay(state, Fight.withWeapon(0)).earned().contains(Achievement.DRAGON_SLAYER));
    }

    @Test
    void bareKnucklesNeedsSevenBarehandedKillsInAWin() {
        GameState lastCard = state().room("2♣").build();
        Progress six = new Progress(Optional.empty(), 0, 6, false, 0, 0, false, 0, Set.of());
        Progress five = new Progress(Optional.empty(), 0, 5, false, 0, 0, false, 0, Set.of());

        assertTrue(after(six, lastCard, Fight.barehanded(0)).earned().contains(Achievement.BARE_KNUCKLES));
        assertFalse(after(five, lastCard, Fight.barehanded(0)).earned().contains(Achievement.BARE_KNUCKLES));
    }

    @Test
    void knockoutNeedsAnAceSlainBarehandedAndAWin() {
        Set<Achievement> earned =
                replay(state().room("A♣").build(), Fight.barehanded(0)).earned();

        assertTrue(earned.contains(Achievement.KNOCKOUT));
    }

    @Test
    void ironmanNeedsAWinWithoutAvoiding() {
        GameState lastCard = state().room("2♣").build();
        Progress avoided = new Progress(Optional.empty(), 0, 0, false, 0, 1, false, 0, Set.of());

        assertTrue(replay(lastCard, Fight.barehanded(0)).earned().contains(Achievement.IRONMAN));
        assertFalse(after(avoided, lastCard, Fight.barehanded(0)).earned().contains(Achievement.IRONMAN));
    }

    @Test
    void lastGaspNeedsAWinWithOneHealth() {
        Set<Achievement> earned = replay(state().health(3).room("2♣").build(), Fight.barehanded(0))
                .earned();

        assertTrue(earned.contains(Achievement.LAST_GASP));
    }

    @Test
    void overflowingChecksThePlainScore() {
        GameState official = state().room("5♥").build();
        GameState fragile = state().rules(RulesConfig.OFFICIAL.withModifiers(Set.of(Modifier.FRAGILE)))
                .room("5♥")
                .build();

        assertTrue(replay(official, new Drink(0)).earned().contains(Achievement.OVERFLOWING));
        assertFalse(replay(fragile, new Drink(0)).earned().contains(Achievement.OVERFLOWING));
    }

    @Test
    void masochistNeedsAWinWithThreeModifiers() {
        Set<Modifier> three = Set.of(Modifier.NO_RETREAT, Modifier.BRITTLE_BLADES, Modifier.FOG);
        Set<Modifier> two = Set.of(Modifier.NO_RETREAT, Modifier.FOG);

        assertTrue(replay(lastCard(three), Fight.barehanded(0)).earned().contains(Achievement.MASOCHIST));
        assertFalse(replay(lastCard(two), Fight.barehanded(0)).earned().contains(Achievement.MASOCHIST));
    }

    @Test
    void nothingThatNeedsAWinComesFromADeath() {
        GameState death = state().health(2).room("A♣ 2♥ 3♥ 9♥").dungeon("5♣").build();

        assertEquals(Set.of(), replay(death, Fight.barehanded(0)).earned());
    }

    @Test
    void earnedSinceListsOnlyTheNewOnes() {
        Progress before = new Progress(Optional.empty(), 0, 0, false, 0, 0, false, 0, Set.of(Achievement.FLUSH));
        Progress after = new Progress(
                Optional.empty(), 0, 0, false, 0, 0, false, 0, Set.of(Achievement.FLUSH, Achievement.IRONMAN));

        assertEquals(List.of(Achievement.IRONMAN), after.earnedSince(before));
    }

    @Test
    void royalFlushNeedsTheTopFiveOfOneSuitInARow() {
        GameState royal =
                state().weapon("10♦", "A♠ K♠ Q♠ J♠").room("10♠ 2♥ 3♥ 9♥").build();
        GameState mixed =
                state().weapon("10♦", "A♠ K♣ Q♠ J♠").room("10♠ 2♥ 3♥ 9♥").build();

        Set<Achievement> earned = replay(royal, Fight.withWeapon(0)).earned();
        assertTrue(earned.contains(Achievement.ROYAL_FLUSH));
        assertTrue(earned.contains(Achievement.STRAIGHT));
        assertFalse(replay(mixed, Fight.withWeapon(0)).earned().contains(Achievement.ROYAL_FLUSH));
    }

    @Test
    void twinsNeedBothMonstersOfARankBackToBack() {
        GameState twins = state().weapon("10♦", "9♣").room("9♠ 2♥ 3♥ 4♥").build();
        GameState later =
                state().weapon("10♦", "9♣ 8♣").room("8♠ 2♥ 3♥ 4♥").dungeon("5♣").build();

        assertTrue(replay(twins, Fight.withWeapon(0)).earned().contains(Achievement.TWINS));
        assertTrue(replay(later, Fight.withWeapon(0)).earned().contains(Achievement.TWINS));
        GameState gap = state().weapon("10♦", "9♣ 8♣").room("7♠ 2♥ 3♥ 4♥").build();
        assertFalse(replay(gap, Fight.withWeapon(0)).earned().contains(Achievement.TWINS));
    }

    @Test
    void aBrittleWeaponsLastKillStillCounts() {
        GameState state = state().rules(RulesConfig.OFFICIAL.withModifiers(Set.of(Modifier.BRITTLE_BLADES)))
                .weapon("10♦", "9♣ 8♣ 7♣")
                .room("7♠ 2♥ 3♥ 4♥")
                .build();

        assertTrue(replay(state, Fight.withWeapon(0)).earned().contains(Achievement.TWINS));
    }

    @Test
    void flawlessNeedsThreeKillsInARoomWithoutDamage() {
        GameState clean =
                state().weapon("10♦").room("9♣ 7♠ 5♣ 2♥").dungeon("3♣ 4♣ 6♣ 8♣").build();
        GameState hurt =
                state().weapon("8♦").room("9♣ 7♠ 5♣ 2♥").dungeon("3♣ 4♣ 6♣ 8♣").build();

        assertTrue(replay(clean, Fight.withWeapon(0), Fight.withWeapon(1), Fight.withWeapon(2))
                .earned()
                .contains(Achievement.FLAWLESS));
        assertFalse(replay(hurt, Fight.withWeapon(0), Fight.withWeapon(1), Fight.withWeapon(2))
                .earned()
                .contains(Achievement.FLAWLESS));
    }

    @Test
    void underdogNeedsTheTwoOfDiamondsToSlayAnAceAndTheBlowSurvived() {
        GameState state = state().room("2♦ A♠ 3♥ 4♥").dungeon("5♣").build();
        GameState weak = state().health(12).room("2♦ A♠ 3♥ 4♥").dungeon("5♣").build();

        assertTrue(replay(state, new Equip(0), Fight.withWeapon(1)).earned().contains(Achievement.UNDERDOG));
        assertFalse(replay(weak, new Equip(0), Fight.withWeapon(1)).earned().contains(Achievement.UNDERDOG));
    }

    @Test
    void escapeArtistNeedsAWinAfterFiveAvoids() {
        GameState lastCard = state().room("2♣").build();
        Progress five = new Progress(Optional.empty(), 0, 0, false, 0, 5, false, 0, Set.of());
        Progress four = new Progress(Optional.empty(), 0, 0, false, 0, 4, false, 0, Set.of());

        assertTrue(after(five, lastCard, Fight.barehanded(0)).earned().contains(Achievement.ESCAPE_ARTIST));
        assertFalse(after(four, lastCard, Fight.barehanded(0)).earned().contains(Achievement.ESCAPE_ARTIST));
    }

    @Test
    void wasteNotNeedsAWinWithoutASecondPotionInAnyRoom() {
        GameState lastCard = state().room("2♣").build();
        Progress wasted = new Progress(Optional.empty(), 0, 0, false, 0, 0, true, 0, Set.of());

        assertTrue(replay(lastCard, Fight.barehanded(0)).earned().contains(Achievement.WASTE_NOT));
        assertFalse(after(wasted, lastCard, Fight.barehanded(0)).earned().contains(Achievement.WASTE_NOT));
    }

    @Test
    void perfectNeedsFullHealthAndTheTenOfHeartsLast() {
        assertTrue(replay(state().room("10♥").build(), new Drink(0)).earned().contains(Achievement.PERFECT));
        assertFalse(replay(state().room("9♥").build(), new Drink(0)).earned().contains(Achievement.PERFECT));
    }

    private static GameState lastCard(Set<Modifier> modifiers) {
        return state().rules(RulesConfig.OFFICIAL.withModifiers(modifiers))
                .room("2♣")
                .build();
    }

    private static Progress replay(GameState state, Action... actions) {
        Progress progress = Progress.start();
        for (Action action : actions) {
            Transition transition = Rules.apply(state, action);
            progress = progress.after(transition);
            state = transition.state();
        }
        return progress;
    }

    private static Progress after(Progress progress, GameState state, Action action) {
        return progress.after(Rules.apply(state, action));
    }
}
