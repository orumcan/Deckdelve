package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.I18NBundle;

/** What every screen shares: the batch, the text bundle, the fonts and the drawing code. */
final class Ui implements Disposable {
    /** The smallest world every layout is designed for, in world units. Wider or taller screens add room around it. */
    static final float WIDTH = 720;

    static final float HEIGHT = 1280;

    final SpriteBatch batch = new SpriteBatch();
    final I18NBundle text;
    final Art art = new Art();
    final Fonts fonts;
    final CardPainter cards;
    Styles styles;

    Ui() {
        // TeaVM lacks java.text.MessageFormat, which the bundle's default formatter needs.
        I18NBundle.setSimpleFormatter(true);
        text = I18NBundle.createBundle(Gdx.files.internal("i18n/strings"));
        fonts = new Fonts(text);
        cards = new CardPainter(art, fonts, text);
    }

    String text(String key, Object... args) {
        return text.format(key, args);
    }

    void resize(int width, int height) {
        float density = Gdx.graphics.getBackBufferWidth() / (float) Gdx.graphics.getWidth();
        float pixelsPerUnit = Math.min(width / WIDTH, height / HEIGHT) * density;
        if (fonts.render(pixelsPerUnit)) {
            styles = new Styles(fonts, art);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        art.dispose();
        fonts.dispose();
    }
}
