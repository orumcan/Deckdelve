package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

final class Styles {
    final LabelStyle title;
    final LabelStyle heading;
    final LabelStyle body;
    final LabelStyle muted;
    final LabelStyle small;
    final LabelStyle link;
    final LabelStyle ink;
    final LabelStyle inkSmall;
    final TextButtonStyle primary;
    final TextButtonStyle secondary;
    final TextButtonStyle quiet;
    final Drawable dim;
    final Drawable panel;
    final Drawable vignette;

    Styles(Fonts fonts, Art art) {
        title = new LabelStyle(fonts.title, Palette.TEXT);
        heading = new LabelStyle(fonts.heading, Palette.TEXT);
        body = new LabelStyle(fonts.body, Palette.TEXT);
        muted = new LabelStyle(fonts.body, Palette.MUTED);
        small = new LabelStyle(fonts.small, Palette.MUTED);
        link = new LabelStyle(fonts.small, Palette.GOLD);
        ink = new LabelStyle(fonts.button, Palette.INK);
        inkSmall = new LabelStyle(fonts.small, Palette.INK);

        primary = button(fonts.button, art, Palette.GOLD, Palette.GOLD_LIP, Palette.INK);
        secondary = button(fonts.button, art, Palette.BUTTON, Palette.BUTTON_LIP, Palette.TEXT);
        quiet = new TextButtonStyle();
        quiet.font = fonts.body;
        quiet.fontColor = Palette.MUTED;
        quiet.overFontColor = Palette.TEXT;

        dim = new TextureRegionDrawable(art.pixel).tint(new Color(0, 0, 0, 0.6f));
        panel = Surfaces.floating(art, Palette.PANEL, Palette.PANEL_BORDER);
        vignette = new TextureRegionDrawable(art.vignette).tint(new Color(0, 0, 0, 0.5f));
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
