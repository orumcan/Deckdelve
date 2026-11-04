package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import java.util.function.IntPredicate;

/** A screen that rebuilds its whole UI from its state on every change and resize, so nothing can fall out of sync. */
abstract class StageScreen extends ScreenAdapter {
    static final float PANEL_WIDTH = 600;

    final Ui ui;
    final Stage stage;
    private Table modal;
    private IntPredicate modalKeys;
    /** Opens the current dialog again after a rebuild, so resizing the window doesn't close it. */
    private Runnable reopen;

    StageScreen(Ui ui) {
        this.ui = ui;
        stage = new Stage(new ExtendViewport(Ui.WIDTH, Ui.HEIGHT), ui.batch);
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return modalKeys != null ? modalKeys.test(keycode) : onKey(keycode);
            }
        });
    }

    abstract void build(Table root);

    boolean onKey(int keycode) {
        return false;
    }

    final void rebuild() {
        // clear() would also drop the key listener on the root.
        stage.getRoot().clearChildren();
        modal = null;
        modalKeys = null;
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ui.styles.vignette);
        stage.addActor(root);
        build(root);
        if (modal == null && reopen != null) {
            reopen.run();
        }
    }

    boolean dialogOpen() {
        return modal != null;
    }

    Table panel() {
        Table panel = new Table();
        panel.setBackground(ui.styles.panel);
        panel.setTouchable(Touchable.enabled);
        panel.pad(36);
        return panel;
    }

    Label heading(String text) {
        Label label = new Label(text, ui.styles.heading);
        label.setWrap(true);
        label.setAlignment(Align.center);
        return label;
    }

    /**
     * Shows a dialog over the dimmed screen. {@code reopen} builds it again after a rebuild; a dialog the screen's state
     * brings back by itself, like the end of a run, passes null.
     */
    void open(Table panel, Runnable reopen, boolean closeOnOutsideClick, IntPredicate keys) {
        overlay(reopen, closeOnOutsideClick, keys).add(panel).width(PANEL_WIDTH);
    }

    /** Like {@link #open}, for a page that takes the screen's full height. */
    void openPage(Table page, Runnable reopen, IntPredicate keys) {
        overlay(reopen, false, keys).add(page).width(Ui.WIDTH).growY();
    }

    private Table overlay(Runnable reopen, boolean closeOnOutsideClick, IntPredicate keys) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(ui.styles.dim);
        overlay.setTouchable(Touchable.enabled);
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
        this.reopen = reopen;
        return overlay;
    }

    void close() {
        reopen = null;
        if (modal != null) {
            modal.remove();
            modal = null;
            modalKeys = null;
        }
    }

    static boolean isBack(int keycode) {
        return keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK;
    }

    static TextButton button(String text, TextButton.TextButtonStyle style, Runnable action) {
        return onChange(new TextButton(text, style), action);
    }

    static <T extends Button> T onChange(T button, Runnable action) {
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        rebuild();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.TABLE);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
