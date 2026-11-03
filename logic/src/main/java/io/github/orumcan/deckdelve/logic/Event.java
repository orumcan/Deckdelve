package io.github.orumcan.deckdelve.logic;

import java.util.List;
import java.util.Optional;

public sealed interface Event {
    record CardDealt(int slot, Card card) implements Event {}

    record RoomAvoided(List<Card> cards) implements Event {
        public RoomAvoided {
            cards = List.copyOf(cards);
        }
    }

    record WeaponEquipped(Card weapon, Optional<Weapon> replaced) implements Event {}

    record PotionDrunk(Card potion, int healed) implements Event {}

    record PotionWasted(Card potion) implements Event {}

    record MonsterFought(Card monster, boolean usedWeapon, int damage) implements Event {}

    record GameOver(Status status, int score) implements Event {}
}
