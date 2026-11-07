package io.github.orumcan.deckdelve.console;

import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Event;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Room;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;
import io.github.orumcan.deckdelve.logic.Status;
import io.github.orumcan.deckdelve.logic.Transition;
import io.github.orumcan.deckdelve.logic.Weapon;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ConsoleGame {
    private final BufferedReader in;
    private final PrintStream out;

    ConsoleGame(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    public static void main(String[] args) throws IOException {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.print(Options.USAGE);
            System.exit(2);
            return;
        }
        if (options.help()) {
            System.out.print(Options.USAGE);
            return;
        }
        if (options.botGames() > 0) {
            BotRuns.run(options.botGames(), options.rules(), System.out);
            return;
        }
        Seed seed = options.seed().orElseGet(() -> Seed.of(new SecureRandom().nextLong()));
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new ConsoleGame(in, out).play(seed, options.rules());
    }

    GameState play(Seed seed, RulesConfig rules) throws IOException {
        out.println("Deckdelve, seed " + seed + (rules.equals(RulesConfig.OFFICIAL) ? "" : ", house rules"));
        GameState state = Rules.start(seed, rules).state();
        while (!state.isOver()) {
            show(state);
            Optional<Action> action = askForAction(state);
            if (action.isEmpty()) {
                out.println("Run abandoned. Seed " + seed + ".");
                return state;
            }
            Transition transition = Rules.apply(state, action.get());
            report(transition.events());
            state = transition.state();
        }
        out.println("Seed " + seed + ".");
        return state;
    }

    private void show(GameState state) {
        out.println();
        out.println("Health " + state.health() + "/" + Rules.maxHealth(state.rules())
                + " · Weapon " + describe(state.weapon())
                + " · Dungeon " + state.dungeon().size()
                + " · Discard " + state.discard().size());
        StringBuilder room = new StringBuilder("Room");
        for (int slot = 0; slot < Room.SIZE; slot++) {
            String card = state.isFaceDown(slot)
                    ? "??"
                    : state.room().card(slot).map(Card::toString).orElse("--");
            room.append("  [").append(slot + 1).append("] ").append(card);
        }
        out.println(room);
    }

    private static String describe(Optional<Weapon> weapon) {
        return weapon.map(w -> w.card()
                        + w.lastSlain().map(last -> " (last slew " + last + ")").orElse(""))
                .orElse("none");
    }

    private Optional<Action> askForAction(GameState state) throws IOException {
        while (true) {
            String command =
                    ask(Rules.canAvoid(state) ? "1-4 pick a card, A avoid, Q quit" : "1-4 pick a card, Q quit");
            if (command == null || command.equals("q")) {
                return Optional.empty();
            }
            if (command.equals("a")) {
                if (Rules.canAvoid(state)) {
                    return Optional.of(new Action.Avoid());
                }
                out.println("This room can't be avoided.");
                continue;
            }
            int slot = command.isEmpty() ? -1 : command.charAt(0) - '1';
            if (slot < 0 || slot >= Room.SIZE) {
                out.println("Type 1-4, A or Q.");
                continue;
            }
            Optional<Card> card = state.room().card(slot);
            if (card.isEmpty()) {
                out.println("Slot " + (slot + 1) + " is empty.");
                continue;
            }
            if (state.isFaceDown(slot)) {
                out.println("That card is face down until you resolve another card in this room.");
                continue;
            }
            String how = command.substring(1);
            switch (card.get().type()) {
                case WEAPON -> {
                    if (how.isEmpty()) {
                        return Optional.of(new Action.Equip(slot));
                    }
                }
                case POTION -> {
                    if (how.isEmpty()) {
                        return Optional.of(new Action.Drink(slot));
                    }
                }
                case MONSTER -> {
                    Card monster = card.get();
                    if (how.isEmpty()) {
                        if (state.weapon().isEmpty()) {
                            return Optional.of(Action.Fight.barehanded(slot));
                        }
                        how = ask(fightChoice(state, monster));
                        if (how == null || how.equals("q")) {
                            return Optional.empty();
                        }
                    }
                    if (how.equals("b")) {
                        return Optional.of(Action.Fight.barehanded(slot));
                    }
                    if (how.equals("w") && Rules.canUseWeapon(state, monster)) {
                        return Optional.of(Action.Fight.withWeapon(slot));
                    }
                    if (how.equals("w")) {
                        out.println(
                                state.weapon().isPresent()
                                        ? "Your weapon can't fight the " + monster + "."
                                        : "You have no weapon.");
                        continue;
                    }
                }
            }
            out.println("Type 1-4, A or Q. For a monster, add W or B, such as 2W.");
        }
    }

    private static String fightChoice(GameState state, Card monster) {
        Weapon weapon = state.weapon().orElseThrow();
        String withWeapon = Rules.canUseWeapon(state, monster)
                ? "W with the " + weapon.card() + " for " + Rules.weaponDamage(weapon, monster, state.rules())
                        + " damage"
                : "the " + weapon.card() + " can't fight it after the "
                        + weapon.lastSlain().orElseThrow();
        return "Fight " + monster + ": " + withWeapon + ", B barehanded for " + monster.value() + " damage";
    }

    private String ask(String prompt) throws IOException {
        out.print(prompt + ": ");
        out.flush();
        String line = in.readLine();
        return line == null ? null : line.trim().toLowerCase(Locale.ROOT);
    }

    private void report(List<Event> events) {
        boolean dealt = false;
        boolean faceDown = false;
        for (Event event : events) {
            if (event instanceof Event.CardDealt card) {
                dealt = true;
                faceDown |= card.faceDown();
            } else if (event instanceof Event.CardRevealed revealed) {
                out.println("The face-down card turns over: " + revealed.card() + ".");
            } else if (event instanceof Event.WeaponBroke broke) {
                out.println("The " + broke.weapon().card() + " breaks." + discarded(broke.weapon()));
            } else if (event instanceof Event.RoomAvoided avoided) {
                out.println("You avoid the room. Its " + avoided.cards().size()
                        + " cards go to the bottom of the dungeon.");
            } else if (event instanceof Event.WeaponEquipped equipped) {
                out.println("You equip the " + equipped.weapon() + "."
                        + equipped.replaced().map(ConsoleGame::discarded).orElse(""));
            } else if (event instanceof Event.PotionDrunk drunk) {
                out.println(
                        drunk.healed() > 0
                                ? "You drink the " + drunk.potion() + " and heal " + drunk.healed() + "."
                                : "You drink the " + drunk.potion() + ", but you're already at full health.");
            } else if (event instanceof Event.PotionWasted wasted) {
                out.println("The " + wasted.potion() + " is wasted: only one potion works per room.");
            } else if (event instanceof Event.MonsterFought fought) {
                out.println("You fight the " + fought.monster()
                        + (fought.usedWeapon() ? " with your weapon" : " barehanded") + " and take " + fought.damage()
                        + " damage.");
            } else if (event instanceof Event.HungerFelt hunger) {
                out.println("Hunger costs you " + hunger.damage() + " health.");
            } else if (event instanceof Event.GameOver over) {
                out.println(
                        over.status() == Status.WON
                                ? "You made it through the dungeon! Score: " + over.score() + "."
                                : "You died. Score: " + over.score() + ".");
            }
        }
        if (dealt) {
            out.println(faceDown ? "A new room is dealt, one card face down." : "A new room is dealt.");
        }
    }

    private static String discarded(Weapon weapon) {
        int monsters = weapon.slain().size();
        if (monsters == 0) {
            return " The " + weapon.card() + " is discarded.";
        }
        return " The " + weapon.card() + " and the " + monsters + (monsters == 1 ? " monster" : " monsters")
                + " on it are discarded.";
    }
}
