package io.github.orumcan.deckdelve.logic;

public sealed interface Action {
    record Avoid() implements Action {}

    record Equip(int slot) implements Action {}

    record Drink(int slot) implements Action {}

    record Fight(int slot, boolean useWeapon) implements Action {
        public static Fight barehanded(int slot) {
            return new Fight(slot, false);
        }

        public static Fight withWeapon(int slot) {
            return new Fight(slot, true);
        }
    }
}
