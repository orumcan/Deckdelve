package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

final class Styles {
    /** How tall a slider's groove is, in world units. */
    private static final float GROOVE = 16;

    private static final float KNOB = 44;

    final LabelStyle title;
    final LabelStyle heading;
    final LabelStyle section;
    final LabelStyle body;
    final LabelStyle strong;
    final LabelStyle muted;
    final LabelStyle small;
    final LabelStyle accent;
    final LabelStyle ink;
    final LabelStyle inkSmall;
    final TextButtonStyle primary;
    final TextButtonStyle secondary;
    /** A tile among several, such as a theme in Settings: the chosen one sits pressed in, in gold. */
    final TextButtonStyle choice;

    final SliderStyle slider;

    final ScrollPaneStyle scroll;
    final Drawable dim;
    final Drawable panel;
    final Drawable tip;
    final Drawable vignette;

    Styles(Fonts fonts, Art art) {
        title = new LabelStyle(fonts.title, Palette.TEXT);
        heading = new LabelStyle(fonts.heading, Palette.TEXT);
        section = new LabelStyle(fonts.button, Palette.GOLD);
        body = new LabelStyle(fonts.body, Palette.TEXT);
        strong = new LabelStyle(fonts.button, Palette.TEXT);
        muted = new LabelStyle(fonts.body, Palette.MUTED);
        small = new LabelStyle(fonts.small, Palette.MUTED);
        accent = new LabelStyle(fonts.small, Palette.GOLD);
        ink = new LabelStyle(fonts.button, Palette.INK);
        inkSmall = new LabelStyle(fonts.small, Palette.INK);

        primary = button(fonts.button, art, Palette.GOLD, Palette.GOLD_LIP, Palette.INK);
        secondary = button(fonts.button, art, Palette.BUTTON, Palette.BUTTON_LIP, Palette.TEXT);
        choice = button(fonts.button, art, Palette.BUTTON, Palette.BUTTON_LIP, Palette.TEXT);
        choice.checked = Surfaces.raised(art, Palette.GOLD, Palette.GOLD_LIP, true);
        choice.checkedDown = choice.checked;
        choice.checkedOffsetY = -Surfaces.PRESS;
        choice.checkedFontColor = Palette.INK;

        slider = new SliderStyle();
        slider.background = groove(art, new Color(0, 0, 0, 0.3f));
        slider.knobBefore = groove(art, Palette.GOLD);
        slider.knob = Surfaces.knob(art, Palette.GOLD, Palette.GOLD_LIP, KNOB);

        scroll = new ScrollPaneStyle();
        TextureRegionDrawable knob = new TextureRegionDrawable(art.pixel);
        scroll.vScrollKnob = knob.tint(new Color(1, 1, 1, 0.25f));
        scroll.vScrollKnob.setMinWidth(6);

        dim = new TextureRegionDrawable(art.pixel).tint(new Color(0, 0, 0, 0.6f));
        panel = Surfaces.floating(art, Palette.PANEL, Palette.PANEL_BORDER);
        tip = Surfaces.floating(art, Palette.PANEL, Palette.GOLD_LIP);
        vignette = new TextureRegionDrawable(art.vignette).tint(new Color(0, 0, 0, 0.5f));
    }

    /** A slider's groove in one color, rounded at both ends. */
    private static Drawable groove(Art art, Color color) {
        NinePatch pill = new NinePatch(art.rounded);
        float scale = GROOVE / 2 / pill.getTopHeight();
        pill.scale(scale, scale);
        NinePatchDrawable groove = new NinePatchDrawable(pill).tint(color);
        groove.setMinWidth(GROOVE);
        groove.setMinHeight(GROOVE);
        return groove;
    }

    private static TextButtonStyle button(BitmapFont font, Art art, Color face, Color lip, Color text) {
        TextButtonStyle style = new TextButtonStyle();
        style.font = font;
        style.up = Surfaces.raised(art, face, lip, false);
        style.over = Surfaces.raised(art, face.cpy().lerp(Color.WHITE, 0.12f), lip, false);
        style.down = Surfaces.raised(art, face, lip, true);
        style.disabled = Surfaces.flat(art, new Color(1, 1, 1, 0.07f));
        style.pressedOffsetY = -Surfaces.PRESS;
        style.fontColor = text;
        style.disabledFontColor = new Color(1, 1, 1, 0.35f);
        return style;
    }
}
