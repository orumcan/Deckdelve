package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.orumcan.deckdelve.Tutorial.Tip;
import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Transition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TutorialTest {
    private final Tutorial tutorial = new Tutorial();
    private GameState state = Rules.start(Tutorial.SEED, RulesConfig.OFFICIAL).state();

    @Test
    void theSeedOpensWithAWeaponTwoMonstersAndAPotion() {
        List<Card> room = state.room().cards();
        assertEquals(1, count(room, CardType.WEAPON));
        assertEquals(2, count(room, CardType.MONSTER));
        assertEquals(1, count(room, CardType.POTION));
        assertEquals(2, count(state.dungeon().subList(0, 3), CardType.POTION));
    }

    @Test
    void eachTipAppearsOnceAtItsMoment() {
        assertEquals(Optional.of(Tip.ROOM), tutorial.start(state));
        assertEquals(Optional.of(Tip.WEAPON), play(new Action.Equip(1)));
        assertEquals(Optional.of(Tip.KILL), play(Action.Fight.withWeapon(2)));
        assertEquals(Optional.of(Tip.AVOID), play(Action.Fight.withWeapon(0)));
        assertEquals(Optional.of(Tip.WASTE), play(new Action.Drink(2)));
        assertEquals(Optional.empty(), play(new Action.Drink(1)));
    }

    @Test
    void avoidingTheOpeningExplainsWhereTheCardsWent() {
        tutorial.start(state);
        assertEquals(Optional.of(Tip.AVOIDED), play(new Action.Avoid()));
    }

    private Optional<Tip> play(Action action) {
        Transition transition = Rules.apply(state, action);
        state = transition.state();
        return tutorial.next(state, transition.events());
    }

    private static long count(List<Card> cards, CardType type) {
        return cards.stream().filter(card -> card.type() == type).count();
    }
}
