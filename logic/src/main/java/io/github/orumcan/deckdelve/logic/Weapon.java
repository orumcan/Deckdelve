package io.github.orumcan.deckdelve.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Weapon(Card card, List<Card> slain) {
    public Weapon {
        if (card.type() != CardType.WEAPON) {
            throw new IllegalArgumentException("Not a weapon: " + card);
        }
        slain = List.copyOf(slain);
    }

    public static Weapon of(Card card) {
        return new Weapon(card, List.of());
    }

    public Optional<Card> lastSlain() {
        return slain.isEmpty() ? Optional.empty() : Optional.of(slain.get(slain.size() - 1));
    }

    public boolean canFight(Card monster, RulesConfig.WeaponRule rule) {
        if (slain.isEmpty()) {
            return true;
        }
        int last = slain.get(slain.size() - 1).value();
        return rule == RulesConfig.WeaponRule.LOWER_OR_EQUAL ? monster.value() <= last : monster.value() < last;
    }

    public int damageAgainst(Card monster) {
        return Math.max(0, monster.value() - card.value());
    }

    Weapon slay(Card monster) {
        List<Card> stack = new ArrayList<>(slain);
        stack.add(monster);
        return new Weapon(card, stack);
    }
}
