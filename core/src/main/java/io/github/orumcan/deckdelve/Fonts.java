package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.I18NBundle;

final class Fonts implements Disposable {
    /**
     * Beyond this many pixels per world unit, text is drawn slightly enlarged instead of rendered bigger. It spares
     * texture memory on very large screens, and gdx-teavm's FreeType fails on glyphs much larger than this.
     */
    private static final float MAX_PIXELS_PER_UNIT = 2;

    private static final String DIGITS = "0123456789";

    BitmapFont title;
    BitmapFont heading;
    BitmapFont button;
    BitmapFont body;
    BitmapFont small;
    BitmapFont index;
    BitmapFont face;

    private final FreeTypeFontGenerator regular =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/Rubik-Regular.ttf"));
    private final FreeTypeFontGenerator bold = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Rubik-Bold.ttf"));
    private final I18NBundle text;
    private float pixelsPerUnit;

    Fonts(I18NBundle text) {
        this.text = text;
    }

    /**
     * Renders every font at its exact pixel size for this screen, so text stays sharp. Returns false when the fonts
     * already match closely enough, which spares a re-render on every step of a window drag.
     */
    boolean render(float pixelsPerUnit) {
        pixelsPerUnit = Math.min(pixelsPerUnit, MAX_PIXELS_PER_UNIT);
        if (title != null && Math.abs(pixelsPerUnit / this.pixelsPerUnit - 1) < 0.15f) {
            return false;
        }
        disposeFonts();
        this.pixelsPerUnit = pixelsPerUnit;
        String ranks = text.get("rank.jack") + text.get("rank.queen") + text.get("rank.king");
        // The large display fonts hold only the characters they draw, which keeps their textures small.
        title = font(bold, 104, text.get("title.name") + DIGITS + "-", 4);
        face = font(bold, 68, ranks, 0);
        index = font(bold, 28, DIGITS + ranks + text.get("rank.ace"), 0);
        heading = font(bold, 46, FreeTypeFontGenerator.DEFAULT_CHARS, 2);
        button = font(bold, 34, FreeTypeFontGenerator.DEFAULT_CHARS, 0);
        body = font(regular, 30, FreeTypeFontGenerator.DEFAULT_CHARS, 0);
        small = font(regular, 24, FreeTypeFontGenerator.DEFAULT_CHARS, 0);
        return true;
    }

    /** Titles and headings get a drop shadow, baked into their glyphs, to lift them off the table. */
    private BitmapFont font(FreeTypeFontGenerator generator, float worldSize, String characters, float shadow) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.max(1, Math.round(worldSize * pixelsPerUnit));
        parameter.characters = characters;
        if (shadow > 0) {
            parameter.shadowOffsetY = Math.max(1, Math.round(shadow * pixelsPerUnit));
            parameter.shadowColor = new Color(0, 0, 0, 0.35f);
        }
        parameter.genMipMaps = true;
        parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        font.getData().setScale(worldSize / parameter.size);
        font.setUseIntegerPositions(false);
        return font;
    }

    private void disposeFonts() {
        for (BitmapFont font : new BitmapFont[] {title, heading, button, body, small, index, face}) {
            if (font != null) {
                font.dispose();
            }
        }
    }

    @Override
    public void dispose() {
        disposeFonts();
        regular.dispose();
        bold.dispose();
    }
}
