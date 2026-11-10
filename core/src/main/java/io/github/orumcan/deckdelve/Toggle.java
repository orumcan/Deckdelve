package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** A switch in a list, such as a modifier in Settings. Off, it's a raised row; on, it's pressed in and gold. */
final class Toggle extends Button {
    private static final float FADED = 0.45f;

    private final Ui ui;
    private final Label title;
    private final Label value;
    private final Label detail;

    /** The value sits at the right of the title, such as a modifier's multiplier, and can be empty. */
    Toggle(Ui ui, String title, String value, String detail, boolean on) {
        super(ui.styles.choice);
        this.ui = ui;
        this.title = new Label(title, ui.styles.strong);
        this.value = new Label(value, ui.styles.strong);
        this.detail = new Label(detail, ui.styles.small);
        this.detail.setWrap(true);
        pad(18, 24, 18 + Surfaces.DEPTH, 24);
        add(this.title).left().expandX();
        add(this.value).right().row();
        add(this.detail).colspan(2).growX().padTop(6);
        setProgrammaticChangeEvents(false);
        setChecked(on);
        recolor();
        addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                recolor();
            }
        });
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        recolor();
    }

    /**
     * Dark text on the gold of a switch that's on, light text on the green of one that's off, and faded text on one
     * that can't be switched.
     */
    private void recolor() {
        boolean on = isChecked();
        title.setStyle(on ? ui.styles.ink : ui.styles.strong);
        value.setStyle(on ? ui.styles.ink : ui.styles.strong);
        detail.setStyle(on ? ui.styles.inkSmall : ui.styles.small);
        float alpha = isDisabled() ? FADED : 1;
        title.getColor().a = alpha;
        value.getColor().a = alpha;
        detail.getColor().a = alpha;
    }
}
