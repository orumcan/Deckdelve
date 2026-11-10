package io.github.orumcan.deckdelve.logic;

import static io.github.orumcan.deckdelve.logic.TestStates.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Action.Fight;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CareerTest {
    @Test
    void hatTrickNeedsThreeWinsInARow() {
        Career career = Career.start().after(win(Set.of())).after(win(Set.of()));
        assertFalse(career.earned().contains(Achievement.HAT_TRICK));

        assertTrue(career.after(win(Set.of())).earned().contains(Achievement.HAT_TRICK));
        assertFalse(career.after(loss()).after(win(Set.of())).earned().contains(Achievement.HAT_TRICK));
    }

    @Test
    void anythingButAWinEndsTheStreak() {
        Career career = Career.start().after(win(Set.of())).after(win(Set.of()));
        GameState left = state().room("2♣ 3♣ 4♣ 5♣").dungeon("6♣").build();

        assertEquals(0, career.after(left).streak());
        assertEquals(2, career.after(left).wins());
    }

    @Test
    void veteranNeedsTenWins() {
        Career career = new Career(9, 0, Set.of(), 1);

        assertFalse(career.earned().contains(Achievement.VETERAN));
        assertTrue(career.after(win(Set.of())).earned().contains(Achievement.VETERAN));
    }

    @Test
    void challengerNeedsAWinWithEachModifierOverAnyNumberOfRuns() {
        Set<Modifier> most = EnumSet.allOf(Modifier.class);
        most.remove(Modifier.FOG);
        Career career = Career.start().after(win(most));
        assertFalse(career.earned().contains(Achievement.CHALLENGER));

        assertTrue(career.after(win(Set.of(Modifier.FOG))).earned().contains(Achievement.CHALLENGER));
    }

    @Test
    void modifiersOpenWithTheFirstWin() {
        assertEquals(0, Career.start().modifierLimit());
        assertEquals(0, Career.start().after(loss()).modifierLimit());
        assertEquals(1, Career.start().after(win(Set.of())).modifierLimit());
    }

    @Test
    void aWinAllowsOneMoreModifierThanItHad() {
        Career career = Career.start().after(win(Set.of())).after(win(Set.of(Modifier.FOG)));
        assertEquals(2, career.modifierLimit());

        assertEquals(3, career.after(win(Set.of(Modifier.FOG, Modifier.THORNS))).modifierLimit());
    }

    @Test
    void theLimitNeverDrops() {
        Career career = Career.start().after(win(Set.of(Modifier.FOG, Modifier.THORNS)));

        assertEquals(3, career.after(win(Set.of())).modifierLimit());
        assertEquals(3, career.after(loss()).modifierLimit());
    }

    @Test
    void theLimitStopsAtEveryModifier() {
        Career career = Career.start().after(win(EnumSet.allOf(Modifier.class)));

        assertEquals(Modifier.values().length, career.modifierLimit());
    }

    @Test
    void modifiersPastTheLimitAreLeftOut() {
        Career career = new Career(1, 1, Set.of(), 2);
        Set<Modifier> chosen = EnumSet.of(Modifier.HUNGER, Modifier.FRAGILE, Modifier.FOG);

        assertEquals(EnumSet.of(Modifier.FRAGILE, Modifier.FOG), career.allowed(chosen));
        assertEquals(Set.of(), Career.start().allowed(chosen));
    }

    private static GameState win(Set<Modifier> modifiers) {
        GameState lastCard = state().rules(RulesConfig.OFFICIAL.withModifiers(modifiers))
                .room("2♣")
                .build();
        return Rules.apply(lastCard, Fight.barehanded(0)).state();
    }

    private static GameState loss() {
        GameState state = state().health(2).room("A♣ 2♥ 3♥ 9♥").dungeon("5♣").build();
        return Rules.apply(state, Fight.barehanded(0)).state();
    }
}
