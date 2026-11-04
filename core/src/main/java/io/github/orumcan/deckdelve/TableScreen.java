package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import io.github.orumcan.deckdelve.logic.Action;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Room;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Status;
import io.github.orumcan.deckdelve.logic.Transition;
import io.github.orumcan.deckdelve.logic.Weapon;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

final class TableScreen extends StageScreen {
    private static final float PAD = 28;
    private static final float PILE_WIDTH = 112;
    private static final float PANEL_WIDTH = 600;

    private final DeckdelveGame game;
    private GameState state;
    private String status;
    private Table modal;
    private IntPredicate modalKeys;
    /** Opens the current dialog again after a rebuild, so resizing the window doesn't close it. */
    private Runnable dialog;

    TableScreen(DeckdelveGame game, Ui ui, GameState state) {
        super(ui);
        this.game = game;
        this.state = state;
        status = ui.text("status.start");
    }

    @Override
    void build(Table root) {
        modal = null;
        modalKeys = null;
        Table column = new Table();
        column.pad(PAD);
        column.defaults().growX();
        column.add(topBar()).row();
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
        if (state.isOver()) {
            showEnd();
        } else if (dialog != null) {
            dialog.run();
        }
    }

    private Table topBar() {
        Table bar = new Table();
        bar.add(new HealthBar(ui, state.health())).growX().height(52);
        bar.add(button(ui.text("table.menu"), ui.styles.secondary, this::showPause))
                .size(150, 64)
                .padLeft(24);
        return bar;
    }

    private Table piles() {
        Table piles = new Table();
        piles.add(pile(state.dungeon().size(), "table.dungeon"));
        Label message = new Label(status, ui.styles.body);
        message.setWrap(true);
        message.setAlignment(Align.center);
        piles.add(message).growX().padLeft(24).padRight(24);
        piles.add(pile(state.discard().size(), "table.discard"));
        return piles;
    }

    /** A face-down pile that looks thicker the more cards it holds, with its count underneath. */
    private Table pile(int count, String key) {
        Group stack = new Group();
        int layers = count == 0 ? 0 : Math.min(3, 1 + count / 15);
        if (layers == 0) {
            stack.addActor(CardActor.slot(ui.cards, PILE_WIDTH));
        }
        for (int layer = layers - 1; layer >= 0; layer--) {
            CardActor back = CardActor.back(ui.cards, PILE_WIDTH);
            back.setPosition(layer * 4, 8 - layer * 4);
            stack.addActor(back);
        }
        Table pile = new Table();
        pile.add(stack).size(PILE_WIDTH + 8, PILE_WIDTH * CardPainter.RATIO + 8).row();
        pile.add(new Label(ui.text(key, count), ui.styles.small)).padTop(10);
        return pile;
    }

    private Table room() {
        Table room = new Table();
        room.defaults().space(20);
        for (int slot = 0; slot < Room.SIZE; slot++) {
            int index = slot;
            CardActor actor = state.room()
                    .card(slot)
                    .map(card -> CardActor.face(ui.cards, card).onClick(() -> pick(index)))
                    .orElseGet(() -> CardActor.slot(ui.cards, CardActor.WIDTH));
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
        float width = Ui.WIDTH - 2 * PAD;
        Group stack = new Group();
        Optional<Weapon> weapon = state.weapon();
        if (weapon.isEmpty()) {
            stack.addActor(CardActor.slot(ui.cards, CardActor.WIDTH));
            Label none = new Label(ui.text("table.noWeapon"), ui.styles.small);
            none.setWrap(true);
            none.setAlignment(Align.center);
            none.setBounds(0, 0, CardActor.WIDTH, CardActor.HEIGHT);
            stack.addActor(none);
        } else {
            stack.addActor(CardActor.face(ui.cards, weapon.get().card()));
            List<Card> slain = weapon.get().slain();
            float start = CardActor.WIDTH * 0.45f;
            float space = width - start - CardActor.WIDTH;
            float step = slain.size() > 1 ? Math.min(CardActor.WIDTH * 0.32f, space / (slain.size() - 1)) : 0;
            for (int i = 0; i < slain.size(); i++) {
                CardActor monster = CardActor.face(ui.cards, slain.get(i));
                monster.setPosition(start + i * step, 0);
                stack.addActor(monster);
            }
        }
        Table area = new Table();
        area.add(new Label(ui.text("table.weapon"), ui.styles.small)).left().row();
        area.add(stack).size(width, CardActor.HEIGHT).padTop(12);
        return area;
    }

    private void pick(int slot) {
        Optional<Card> card = state.room().card(slot);
        if (modal != null || state.isOver() || card.isEmpty()) {
            return;
        }
        switch (card.get().type()) {
            case WEAPON -> apply(new Action.Equip(slot));
            case POTION -> apply(new Action.Drink(slot));
            case MONSTER -> showFight(slot, card.get());
        }
    }

    private void apply(Action action) {
        dialog = null;
        Transition transition = Rules.apply(state, action);
        state = transition.state();
        status = StatusText.of(transition.events(), ui);
        rebuild();
    }

    @Override
    boolean onKey(int keycode) {
        if (modalKeys != null) {
            return modalKeys.test(keycode);
        }
        int slot = slotFor(keycode);
        if (slot >= 0) {
            pick(slot);
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

    private static int slotFor(int keycode) {
        if (keycode >= Input.Keys.NUM_1 && keycode < Input.Keys.NUM_1 + Room.SIZE) {
            return keycode - Input.Keys.NUM_1;
        }
        if (keycode >= Input.Keys.NUMPAD_1 && keycode < Input.Keys.NUMPAD_1 + Room.SIZE) {
            return keycode - Input.Keys.NUMPAD_1;
        }
        return -1;
    }

    private static boolean isBack(int keycode) {
        return keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK;
    }

    /** Asks every time, even without a weapon, so a stray tap on a monster never costs health. */
    private void showFight(int slot, Card monster) {
        dialog = () -> showFight(slot, monster);
        boolean canUseWeapon = Rules.canUseWeapon(state, monster);
        Runnable withWeapon = () -> apply(Action.Fight.withWeapon(slot));
        Runnable barehanded = () -> apply(Action.Fight.barehanded(slot));
        Table panel = panel();
        panel.add(new Label(ui.text("fight.title"), ui.styles.heading)).row();
        panel.add(CardActor.face(ui.cards, monster))
                .size(CardActor.WIDTH, CardActor.HEIGHT)
                .padTop(20)
                .row();
        panel.add(choice(ui.text("fight.weapon"), weaponDetail(monster), canUseWeapon, withWeapon))
                .growX()
                .padTop(32)
                .row();
        panel.add(choice(ui.text("fight.bare"), ui.text("fight.damage", monster.value()), true, barehanded))
                .growX()
                .padTop(16)
                .row();
        panel.add(button(ui.text("fight.cancel"), ui.styles.quiet, this::close)).padTop(16);
        open(panel, true, keycode -> {
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
    }

    private String weaponDetail(Card monster) {
        Optional<Weapon> weapon = state.weapon();
        if (weapon.isEmpty()) {
            return ui.text("fight.noWeapon");
        }
        if (Rules.canUseWeapon(state, monster)) {
            return ui.text("fight.damage", weapon.get().damageAgainst(monster));
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
        dialog = this::showPause;
        Table panel = panel();
        panel.add(wrapped(ui.text("pause.title"))).growX().row();
        panel.add(button(ui.text("pause.resume"), ui.styles.primary, this::close))
                .growX()
                .height(92)
                .padTop(36)
                .row();
        // A fresh dungeon in one tap, so a bad opening never means a trip through the title screen.
        panel.add(button(ui.text("pause.newRun"), ui.styles.secondary, game::startRun))
                .growX()
                .height(84)
                .padTop(16)
                .row();
        panel.add(button(ui.text("pause.quit"), ui.styles.secondary, game::showTitle))
                .growX()
                .height(84)
                .padTop(16);
        open(panel, true, keycode -> {
            if (isBack(keycode)) {
                close();
                return true;
            }
            return false;
        });
    }

    private void showEnd() {
        Table panel = panel();
        panel.add(wrapped(ui.text(state.status() == Status.WON ? "end.won" : "end.lost")))
                .growX()
                .row();
        panel.add(new Label(ui.text("end.score"), ui.styles.muted)).padTop(20).row();
        panel.add(new Label(Integer.toString(Scoring.score(state)), ui.styles.title))
                .row();
        panel.add(new Label(ui.text("end.seed", state.seed()), ui.styles.small)).row();
        panel.add(button(ui.text("end.again"), ui.styles.primary, game::startRun))
                .growX()
                .height(92)
                .padTop(36)
                .row();
        panel.add(button(ui.text("end.title"), ui.styles.secondary, game::showTitle))
                .growX()
                .height(84)
                .padTop(16);
        open(panel, false, keycode -> {
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                game.startRun();
            } else if (isBack(keycode)) {
                game.showTitle();
            } else {
                return false;
            }
            return true;
        });
    }

    private Label wrapped(String text) {
        Label label = new Label(text, ui.styles.heading);
        label.setWrap(true);
        label.setAlignment(Align.center);
        return label;
    }

    private Table panel() {
        Table panel = new Table();
        panel.setBackground(ui.styles.panel);
        panel.setTouchable(Touchable.enabled);
        panel.pad(36);
        return panel;
    }

    private void open(Table panel, boolean closeOnOutsideClick, IntPredicate keys) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(ui.styles.dim);
        overlay.setTouchable(Touchable.enabled);
        overlay.add(panel).width(PANEL_WIDTH);
        if (closeOnOutsideClick) {
            overlay.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (event.getTarget() == overlay) {
                        close();
                    }
                }
            });
        }
        stage.addActor(overlay);
        modal = overlay;
        modalKeys = keys;
    }

    private void close() {
        dialog = null;
        modal.remove();
        modal = null;
        modalKeys = null;
    }
}
