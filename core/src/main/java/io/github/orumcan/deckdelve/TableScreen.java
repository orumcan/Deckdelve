package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import io.github.orumcan.deckdelve.logic.Achievement;
import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Event;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.Progress;
import io.github.orumcan.deckdelve.logic.Room;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Status;
import io.github.orumcan.deckdelve.logic.Transition;
import io.github.orumcan.deckdelve.logic.Weapon;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

final class TableScreen extends StageScreen {
    private static final float PAD = 28;
    private static final float END_FADE = 0.25f;
    /** Seconds, longer than any animation takes. */
    private static final float FOREVER = 60;

    private final DeckdelveGame game;
    /** Null in a normal run. */
    private final Tutorial tutorial;

    private GameState state;
    private final List<Action> actions = new ArrayList<>();
    private String status;
    private Optional<Tutorial.Tip> tip;
    private Progress progress = Progress.start();
    /** The achievements this run unlocked for the first time, listed when it ends. */
    private final List<Achievement> unlocked = new ArrayList<>();
    /** The ones the last action unlocked, shown in the banner until the next. */
    private List<Achievement> fresh = List.of();
    /** Whether a card has been resolved, which makes the run count even if it's dropped early. */
    private boolean started;

    private HealthBar health;
    /** The compact layout (D59) shows the Room's slots in its own order, set as each room is dealt. */
    private final boolean compact;
    /** The Room slot shown at each place, left to right. */
    private int[] order = {0, 1, 2, 3};

    private final AnimationSpeed speed;
    /** Every card on the table in the latest layout, so an animation can find where each one went. */
    private final Map<Card, CardActor> placed = new LinkedHashMap<>();

    private Pile dungeon;
    private Pile discard;
    /** The weapon and the kills stacked on it. */
    private Group weaponStack;
    /** Cards in flight, above the table and below any dialog. */
    private Group layer;
    /** While an animation plays, a tap or key press only finishes it (D58). */
    private boolean animating;
    /** The events that dealt the first room, kept until the table shows them being dealt. */
    private List<Event> opening = List.of();

    TableScreen(DeckdelveGame game, Ui ui, SavedRun run) {
        this(game, ui, run, null);
    }

    /** Replays the run's actions, so a saved run comes back with its achievement progress. */
    TableScreen(DeckdelveGame game, Ui ui, SavedRun run, Tutorial tutorial) {
        super(ui);
        this.game = game;
        this.tutorial = tutorial;
        compact = game.profile().compactRoom();
        speed = game.profile().animationSpeed();
        List<Transition> history = run.replay();
        for (Transition transition : history.subList(1, history.size())) {
            progress = progress.after(transition);
        }
        for (Transition transition : history) {
            arrange(transition);
        }
        Transition last = history.get(history.size() - 1);
        state = last.state();
        if (run.actions().isEmpty()) {
            opening = history.get(0).events();
        }
        for (Action action : run.actions()) {
            actions.add(action);
            started |= !(action instanceof Action.Avoid);
        }
        unlocked.addAll(run.unlocked());
        if (!actions.isEmpty()) {
            status = StatusText.of(last.events(), ui);
        } else if (tutorial != null) {
            // The opening tip already says what the status line would.
            status = "";
        } else {
            status = ui.text(state.rules().has(Modifier.FULL_CLEAR) ? "status.startFullClear" : "status.start");
        }
        tip = tutorial == null ? Optional.empty() : tutorial.start(state);
        stage.addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return skip(event);
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return skip(event);
            }
        });
    }

    @Override
    void build(Table root) {
        placed.clear();
        Table column = new Table();
        column.pad(PAD);
        column.defaults().growX();
        column.add(topBar()).row();
        Set<Modifier> modifiers = state.rules().modifiers();
        if (!modifiers.isEmpty()) {
            Label line = new Label(
                    ui.text(
                            "table.modifiers",
                            ui.modifierNames(modifiers),
                            ui.multiplier(Scoring.multiplier(modifiers))),
                    ui.styles.small);
            line.setAlignment(Align.center);
            column.add(line).padTop(14).row();
        }
        tip.ifPresent(shown -> column.add(tipBanner(shown)).padTop(24).row());
        if (!fresh.isEmpty()) {
            column.add(achievementBanner()).padTop(24).row();
        }
        column.add().expandY().row();
        column.add(piles()).row();
        column.add().expandY().row();
        column.add(room()).row();
        column.add().expandY().row();
        column.add(weapon()).row();
        column.add().expandY().row();
        TextButton avoid = button(ui.text("table.avoid"), ui.styles.secondary, () -> apply(new Action.Avoid()));
        avoid.setDisabled(!Rules.canAvoid(state));
        column.add(avoid).height(92);
        root.add(column).width(Ui.WIDTH).growY();
        layer = new Group();
        layer.setTouchable(Touchable.disabled);
        stage.addActor(layer);
        if (state.isOver() && !animating) {
            showEnd();
        }
    }

    private Table tipBanner(Tutorial.Tip shown) {
        Table banner = new Table();
        banner.setBackground(ui.styles.tip);
        banner.pad(18, 24, 20, 24);
        banner.add(new Label(ui.text("tutorial.tip"), ui.styles.accent)).left().row();
        Label text = new Label(ui.text(shown.key), ui.styles.body);
        text.setWrap(true);
        banner.add(text).growX().padTop(6);
        return banner;
    }

    private Table achievementBanner() {
        Table banner = new Table();
        banner.setBackground(ui.styles.tip);
        banner.pad(18, 24, 20, 24);
        banner.defaults().left().growX();
        banner.add(new Label(ui.text("achievement.unlocked"), ui.styles.accent)).row();
        for (Achievement achievement : fresh) {
            banner.add(new Label(ui.text(Ui.key("achievement", achievement)), ui.styles.strong))
                    .padTop(8)
                    .row();
            Label body = new Label(ui.text(Ui.key("achievement", achievement) + ".body"), ui.styles.small);
            body.setWrap(true);
            banner.add(body).padTop(2).row();
        }
        return banner;
    }

    private Table topBar() {
        Table bar = new Table();
        health = new HealthBar(ui, state.health(), Rules.maxHealth(state.rules()), speed.moves());
        bar.add(health).growX().height(52);
        bar.add(button(ui.text("table.menu"), ui.styles.secondary, this::showPause))
                .size(150, 64)
                .padLeft(24);
        return bar;
    }

    private Table piles() {
        Table piles = new Table();
        dungeon = new Pile(ui, "table.dungeon", state.dungeon().size());
        discard = new Pile(ui, "table.discard", state.discard().size());
        piles.add(dungeon);
        Label message = new Label(status, ui.styles.body);
        message.setWrap(true);
        message.setAlignment(Align.center);
        piles.add(message).growX().padLeft(24).padRight(24);
        piles.add(discard);
        return piles;
    }

    private Table room() {
        Table room = new Table();
        room.defaults().space(20);
        for (int place = 0; place < Room.SIZE; place++) {
            int index = order[place];
            CardActor actor = state.room()
                    .card(index)
                    .map(card -> state.isFaceDown(index)
                            ? CardActor.back(ui.cards, CardActor.WIDTH).onClick(() -> pick(index))
                            : CardActor.face(ui.cards, card).onClick(() -> pick(index)))
                    .orElseGet(() -> CardActor.slot(ui.cards, CardActor.WIDTH));
            state.room().card(index).ifPresent(card -> placed.put(card, actor));
            preview(index).ifPresent(action -> previewWhileOver(actor, health, action));
            room.add(actor).size(CardActor.WIDTH, CardActor.HEIGHT);
        }
        if (Gdx.input.isPeripheralAvailable(Input.Peripheral.HardwareKeyboard)) {
            room.row();
            for (int slot = 0; slot < Room.SIZE; slot++) {
                room.add(new Label(Integer.toString(slot + 1), ui.styles.small)).spaceTop(10);
            }
        }
        return room;
    }

    /** The weapon with every monster it slew stacked across it, newest on top, like on the physical table. */
    private Table weapon() {
        weaponStack = new Group();
        Optional<Weapon> weapon = state.weapon();
        if (weapon.isEmpty()) {
            weaponStack.addActor(CardActor.slot(ui.cards, CardActor.WIDTH));
            Label none = new Label(ui.text("table.noWeapon"), ui.styles.small);
            none.setWrap(true);
            none.setAlignment(Align.center);
            none.setBounds(0, 0, CardActor.WIDTH, CardActor.HEIGHT);
            weaponStack.addActor(none);
        } else {
            CardActor card = CardActor.face(ui.cards, weapon.get().card());
            placed.put(weapon.get().card(), card);
            weaponStack.addActor(card);
            List<Card> slain = weapon.get().slain();
            for (int i = 0; i < slain.size(); i++) {
                CardActor monster = CardActor.face(ui.cards, slain.get(i));
                monster.setPosition(stackX(i, slain.size()), 0);
                placed.put(slain.get(i), monster);
                weaponStack.addActor(monster);
            }
        }
        Table area = new Table();
        area.add(new Label(ui.text("table.weapon"), ui.styles.small)).left().row();
        area.add(weaponStack).size(Ui.WIDTH - 2 * PAD, CardActor.HEIGHT).padTop(12);
        return area;
    }

    /** Where a kill sits across the weapon, among so many: closer together as more pile up, to fit the table. */
    private static float stackX(int index, int count) {
        float start = CardActor.WIDTH * 0.45f;
        float space = Ui.WIDTH - 2 * PAD - start - CardActor.WIDTH;
        float step = count > 1 ? Math.min(CardActor.WIDTH * 0.32f, space / (count - 1)) : 0;
        return start + index * step;
    }

    /** What pointing at a Room card previews: a fight with the weapon when it can fight, barehanded otherwise. */
    private Optional<Action> preview(int slot) {
        Optional<Card> card = state.room().card(slot);
        if (state.isOver() || card.isEmpty() || state.isFaceDown(slot)) {
            return Optional.empty();
        }
        return switch (card.get().type()) {
            case WEAPON -> Optional.of(new Action.Equip(slot));
            case POTION -> Optional.of(new Action.Drink(slot));
            case MONSTER ->
                Optional.of(
                        Rules.canUseWeapon(state, card.get())
                                ? Action.Fight.withWeapon(slot)
                                : Action.Fight.barehanded(slot));
        };
    }

    /**
     * Shades on the bar what an action would cost or heal while the pointer is over the actor, or a finger holds it.
     * Actions that wouldn't change health, like a second potion in a room, shade nothing.
     */
    private void previewWhileOver(Actor actor, HealthBar bar, Action action) {
        int after = healthAfter(action);
        if (after == state.health()) {
            return;
        }
        actor.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (fromActor == null || !fromActor.isDescendantOf(actor)) {
                    bar.preview(after);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (toActor == null || !toActor.isDescendantOf(actor)) {
                    bar.clearPreview();
                }
            }
        });
    }

    /**
     * The health an action would leave, as the engine works it out. That's the action's own doing, without the Hunger
     * of a room it deals, unless the run would end: a death always shows, whatever deals it.
     */
    private int healthAfter(Action action) {
        Transition result = Rules.apply(state, action);
        if (result.state().status() == Status.LOST) {
            return result.state().health();
        }
        for (Event event : result.events()) {
            if (event instanceof Event.MonsterFought fought) {
                return state.health() - fought.damage();
            }
            if (event instanceof Event.PotionDrunk drunk) {
                return state.health() + drunk.healed();
            }
        }
        return state.health();
    }

    /** A fight's damage, marked when it would end the run: by the blow, or by Hunger as the next room is dealt. */
    private String damage(Action.Fight fight, int damage) {
        if (healthAfter(fight) > 0) {
            return ui.text("fight.damage", damage);
        }
        return ui.text(damage < state.health() ? "fight.fatalHunger" : "fight.fatal", damage);
    }

    private void pick(int slot) {
        Optional<Card> card = state.room().card(slot);
        if (dialogOpen() || state.isOver() || card.isEmpty()) {
            return;
        }
        if (state.isFaceDown(slot)) {
            status = ui.text("status.faceDown");
            rebuild();
            return;
        }
        switch (card.get().type()) {
            case WEAPON -> apply(new Action.Equip(slot));
            case POTION -> apply(new Action.Drink(slot));
            case MONSTER -> showFight(slot, card.get());
        }
    }

    private void apply(Action action) {
        finish();
        close();
        Optional<TableAnimator.Before> before = speed.moves() ? Optional.of(before()) : Optional.empty();
        Transition transition = Rules.apply(state, action);
        state = transition.state();
        actions.add(action);
        arrange(transition);
        status = StatusText.of(transition.events(), ui);
        if (tutorial != null) {
            tip = tutorial.next(state, transition.events());
        } else {
            track(action, transition);
        }
        animating = before.isPresent();
        rebuild();
        if (animating) {
            layOut();
            new TableAnimator(ui, ui.sounds, layer, speed, before.get(), placed, dungeon, discard, health)
                    .play(transition.events(), this::settle);
        } else {
            soundAtOnce(transition.events());
        }
    }

    /** With animation off, an action's sounds play together, each once. */
    private void soundAtOnce(List<Event> events) {
        Set<Sounds.Effect> effects = EnumSet.noneOf(Sounds.Effect.class);
        for (Event event : events) {
            Sounds.Effect.of(event).ifPresent(effects::add);
        }
        for (Sounds.Effect effect : effects) {
            ui.sounds.play(effect);
        }
    }

    /** Where everything on the table is now, for the next action's animation to start from. */
    private TableAnimator.Before before() {
        layOut();
        Map<Card, Rectangle> cards = new HashMap<>();
        placed.forEach((card, actor) -> cards.put(card, TableAnimator.bounds(actor)));
        Optional<Rectangle> nextKill = state.weapon().map(weapon -> {
            int kills = weapon.slain().size();
            Vector2 corner = weaponStack.localToStageCoordinates(new Vector2(stackX(kills, kills + 1), 0));
            return new Rectangle(corner.x, corner.y, CardActor.WIDTH, CardActor.HEIGHT);
        });
        return new TableAnimator.Before(state, cards, nextKill);
    }

    /** The animation is over: the end of a run now brings its panel in. */
    private void settle() {
        animating = false;
        if (state.isOver()) {
            Table overlay = showEnd();
            overlay.getColor().a = 0;
            overlay.addAction(Actions.fadeIn(END_FADE * speed.scale));
        }
    }

    /** Plays a running animation to its end at once. */
    private void finish() {
        // Each step completes at least one part of every animation still running, and none has more than a few.
        for (int step = 0; step < 100 && animating; step++) {
            stage.act(FOREVER);
        }
        animating = false;
    }

    /** A tap or key press during an animation only finishes it, so it can't land on a card still flying in. */
    private boolean skip(InputEvent event) {
        if (!animating) {
            return false;
        }
        finish();
        event.stop();
        return true;
    }

    @Override
    public void resize(int width, int height) {
        finish();
        super.resize(width, height);
        if (!opening.isEmpty() && speed.moves()) {
            dealOpening();
        }
        opening = List.of();
    }

    /** The first room dealt card by card from the Dungeon pile, as the table first shows a run with no card taken. */
    private void dealOpening() {
        List<Card> dungeon = new ArrayList<>(state.room().cards());
        dungeon.addAll(state.dungeon());
        GameState undealt = new GameState(
                state.seed(),
                state.rules(),
                dungeon,
                Room.empty(),
                OptionalInt.empty(),
                state.health(),
                state.weapon(),
                state.discard(),
                0,
                0,
                false,
                false,
                Optional.empty(),
                Status.PLAYING);
        animating = true;
        layOut();
        TableAnimator.Before before = new TableAnimator.Before(undealt, Map.of(), Optional.empty());
        new TableAnimator(ui, ui.sounds, layer, speed, before, placed, this.dungeon, discard, health)
                .play(opening, this::settle);
    }

    /** Lays the compact layout out again whenever a room is dealt. Within a room, nothing moves. */
    private void arrange(Transition transition) {
        List<Integer> dealt = new ArrayList<>();
        for (Event event : transition.events()) {
            if (event instanceof Event.CardDealt card) {
                dealt.add(card.slot());
            }
        }
        if (compact && !dealt.isEmpty()) {
            order = compactOrder(transition.state().room(), dealt);
        }
    }

    /**
     * The slot shown at each place in the compact layout (D59), for a room just dealt into the given slots: the card
     * left from the last room first, then the new cards in the order they were dealt, which is their slot order.
     */
    static int[] compactOrder(Room room, List<Integer> dealt) {
        int[] order = new int[Room.SIZE];
        int place = 0;
        for (int slot = 0; slot < Room.SIZE; slot++) {
            if (room.card(slot).isPresent() && !dealt.contains(slot)) {
                order[place++] = slot;
            }
        }
        for (int slot : dealt) {
            order[place++] = slot;
        }
        for (int slot = 0; slot < Room.SIZE; slot++) {
            if (room.card(slot).isEmpty()) {
                order[place++] = slot;
            }
        }
        return order;
    }

    /** Saves every run but the tutorial after each action, and keeps its achievements and stats. */
    private void track(Action action, Transition transition) {
        started |= !(action instanceof Action.Avoid);
        Progress next = progress.after(transition);
        fresh = new ArrayList<>();
        unlock(next.earnedSince(progress));
        progress = next;
        if (state.isOver()) {
            game.profile().dropRun();
            Optional<Card> killer = state.status() == Status.LOST ? killer(transition.events()) : Optional.empty();
            unlock(game.profile().record(state, killer));
        } else {
            game.profile().save(new SavedRun(state.seed(), state.rules(), actions, unlocked));
        }
    }

    /** Shows the achievements unlocked for the first time in the banner, and lists them at the end of the run. */
    private void unlock(Iterable<Achievement> earned) {
        for (Achievement achievement : earned) {
            if (game.profile().unlock(achievement)) {
                fresh.add(achievement);
                unlocked.add(achievement);
            }
        }
    }

    /** The monster that dealt a death's last blow, if a monster did: Hunger can end a run too. */
    private static Optional<Card> killer(List<Event> events) {
        Optional<Card> killer = Optional.empty();
        for (Event event : events) {
            if (event instanceof Event.MonsterFought fought) {
                killer = Optional.of(fought.monster());
            } else if (event instanceof Event.HungerFelt) {
                killer = Optional.empty();
            }
        }
        return killer;
    }

    /** Drops the run for a new one, which counts it once a card has been resolved. */
    private void startOver() {
        if (started && !state.isOver()) {
            game.profile().record(state, Optional.empty());
        }
        game.startRun();
    }

    @Override
    boolean onKey(int keycode) {
        int place = placeFor(keycode);
        if (place >= 0) {
            pick(order[place]);
            return true;
        }
        if (keycode == Input.Keys.A && Rules.canAvoid(state)) {
            apply(new Action.Avoid());
            return true;
        }
        if (isBack(keycode)) {
            showPause();
            return true;
        }
        return false;
    }

    /** The place in the Room a number key picks, counted from the left as the keys are shown. */
    private static int placeFor(int keycode) {
        if (keycode >= Input.Keys.NUM_1 && keycode < Input.Keys.NUM_1 + Room.SIZE) {
            return keycode - Input.Keys.NUM_1;
        }
        if (keycode >= Input.Keys.NUMPAD_1 && keycode < Input.Keys.NUMPAD_1 + Room.SIZE) {
            return keycode - Input.Keys.NUMPAD_1;
        }
        return -1;
    }

    /** Asks every time, even without a weapon, so a stray tap on a monster never costs health. */
    private void showFight(int slot, Card monster) {
        boolean canUseWeapon = Rules.canUseWeapon(state, monster);
        Runnable withWeapon = () -> apply(Action.Fight.withWeapon(slot));
        Runnable barehanded = () -> apply(Action.Fight.barehanded(slot));
        Button weaponChoice = choice(ui.text("fight.weapon"), weaponDetail(slot, monster), canUseWeapon, withWeapon);
        Button bareChoice =
                choice(ui.text("fight.bare"), damage(Action.Fight.barehanded(slot), monster.value()), true, barehanded);
        Table panel = panel();
        panel.add(new Label(ui.text("fight.title"), ui.styles.heading)).row();
        panel.add(CardActor.face(ui.cards, monster))
                .size(CardActor.WIDTH, CardActor.HEIGHT)
                .padTop(20)
                .row();
        panel.add(weaponChoice).growX().padTop(32).row();
        panel.add(bareChoice).growX().padTop(16).row();
        panel.add(button(ui.text("fight.cancel"), ui.styles.secondary, this::close))
                .growX()
                .height(84)
                .padTop(16);
        Table overlay = open(panel, () -> showFight(slot, monster), true, keycode -> {
            if (keycode == Input.Keys.W && canUseWeapon) {
                withWeapon.run();
            } else if (keycode == Input.Keys.B) {
                barehanded.run();
            } else if (isBack(keycode)) {
                close();
            } else {
                return false;
            }
            return true;
        });
        HealthBar lifted = liftHealthBar(overlay);
        if (canUseWeapon) {
            previewWhileOver(weaponChoice, lifted, Action.Fight.withWeapon(slot));
        }
        previewWhileOver(bareChoice, lifted, Action.Fight.barehanded(slot));
    }

    /**
     * A copy of the health bar over the dimmed table, in the same place, so the fight's choices can shade it. The bar
     * below hides until the dialog closes, or it would show through the copy's faded part.
     */
    private HealthBar liftHealthBar(Table overlay) {
        layOut();
        Vector2 corner = health.localToStageCoordinates(new Vector2());
        HealthBar lifted = new HealthBar(ui, state.health(), Rules.maxHealth(state.rules()), speed.moves());
        lifted.setBounds(corner.x, corner.y, health.getWidth(), health.getHeight());
        overlay.addActor(lifted);
        health.setVisible(false);
        return lifted;
    }

    @Override
    void close() {
        super.close();
        if (health != null) {
            health.setVisible(true);
        }
    }

    private String weaponDetail(int slot, Card monster) {
        Optional<Weapon> weapon = state.weapon();
        if (weapon.isEmpty()) {
            return ui.text("fight.noWeapon");
        }
        if (Rules.canUseWeapon(state, monster)) {
            return damage(Action.Fight.withWeapon(slot), Rules.weaponDamage(weapon.get(), monster, state.rules()));
        }
        return ui.text("fight.blocked", ui.cards.rank(weapon.get().lastSlain().orElseThrow()));
    }

    private Button choice(String title, String detail, boolean enabled, Runnable action) {
        Button button = new Button(ui.styles.primary);
        button.pad(14, 16, 14 + Surfaces.DEPTH, 16);
        button.add(new Label(title, enabled ? ui.styles.ink : ui.styles.muted)).row();
        button.add(new Label(detail, enabled ? ui.styles.inkSmall : ui.styles.small))
                .padTop(4);
        button.setDisabled(!enabled);
        return onChange(button, action);
    }

    private void showPause() {
        Table panel = panel();
        panel.add(heading(ui.text("pause.title"))).growX().row();
        panel.add(button(ui.text("pause.resume"), ui.styles.primary, this::close))
                .growX()
                .height(92)
                .padTop(36)
                .row();
        panel.add(button(ui.text("pause.rules"), ui.styles.secondary, this::showRules))
                .growX()
                .height(84)
                .padTop(16)
                .row();
        // A fresh dungeon in one tap, so a bad opening never means a trip through the title screen.
        TextButton newRun = tutorial == null
                ? button(ui.text("pause.newRun"), ui.styles.secondary, this::startOver)
                : button(ui.text("pause.realRun"), ui.styles.secondary, game::play);
        panel.add(newRun).growX().height(84).padTop(16).row();
        // A real run stays saved for Continue on the title screen.
        panel.add(button(ui.text("pause.quit"), ui.styles.secondary, game::showTitle))
                .growX()
                .height(84)
                .padTop(16);
        open(panel, this::showPause, true, keycode -> {
            if (isBack(keycode)) {
                close();
                return true;
            }
            return false;
        });
    }

    /** The rules over the table, so a run can be checked against them without leaving it. */
    private void showRules() {
        close();
        ScrollPane scroll = Pages.scroller(ui, HowToPlay.content(ui, null));
        Table page = Pages.frame(ui, ui.text("rules.title"), scroll, this::close);
        page.setBackground(ui.styles.panel);
        openPage(page, this::showRules, keycode -> {
            if (isBack(keycode)) {
                close();
                return true;
            }
            return Pages.scrollByKey(scroll, keycode);
        });
        stage.setScrollFocus(scroll);
    }

    private Table showEnd() {
        Table panel = panel();
        panel.add(heading(ui.text(state.status() == Status.WON ? "end.won" : "end.lost")))
                .growX()
                .row();
        panel.add(new Label(ui.text("end.score"), ui.styles.muted)).padTop(20).row();
        panel.add(new Label(Ui.number(Scoring.score(state)), ui.styles.title)).row();
        Set<Modifier> modifiers = state.rules().modifiers();
        if (!modifiers.isEmpty()) {
            if (state.status() == Status.WON) {
                String multiplier = Ui.decimal(Scoring.multiplier(modifiers));
                panel.add(new Label(ui.text("end.multiplied", Scoring.plainScore(state), multiplier), ui.styles.muted))
                        .row();
            }
            Label names = new Label(ui.text("end.modifiers", ui.modifierNames(modifiers)), ui.styles.small);
            names.setWrap(true);
            names.setAlignment(Align.center);
            panel.add(names).growX().padTop(8).row();
        }
        panel.add(new Label(ui.text("end.seed", state.seed()), ui.styles.small))
                .padTop(8)
                .row();
        if (!unlocked.isEmpty()) {
            panel.add(new Label(ui.text("end.achievements"), ui.styles.muted))
                    .padTop(24)
                    .row();
            for (Achievement achievement : unlocked) {
                panel.add(new Label(ui.text(Ui.key("achievement", achievement)), ui.styles.section))
                        .padTop(6)
                        .row();
            }
        }
        String again = tutorial == null ? "end.again" : "end.realRun";
        Runnable next = tutorial == null ? game::startRun : game::play;
        panel.add(button(ui.text(again), ui.styles.primary, next))
                .growX()
                .height(92)
                .padTop(36)
                .row();
        panel.add(button(ui.text("end.title"), ui.styles.secondary, game::showTitle))
                .growX()
                .height(84)
                .padTop(16);
        return open(panel, null, false, keycode -> {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                next.run();
            } else if (isBack(keycode)) {
                game.showTitle();
            } else {
                return false;
            }
            return true;
        });
    }
}
