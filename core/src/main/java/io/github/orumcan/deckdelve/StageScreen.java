package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** A screen that rebuilds its whole UI from its state on every change and resize, so nothing can fall out of sync. */
abstract class StageScreen extends ScreenAdapter {
    final Ui ui;
    final Stage stage;

    StageScreen(Ui ui) {
        this.ui = ui;
        stage = new Stage(new ExtendViewport(Ui.WIDTH, Ui.HEIGHT), ui.batch);
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return onKey(keycode);
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
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ui.styles.vignette);
        stage.addActor(root);
        build(root);
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
