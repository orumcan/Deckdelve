package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.I18NBundle;
import java.util.HashMap;
import java.util.Map;

/** What every screen shares: the batch, the text bundle, the fonts, the card theme and the drawing code. */
final class Ui implements Disposable {
    /** The smallest world every layout is designed for, in world units. Wider or taller screens add room around it. */
    static final float WIDTH = 720;

    static final float HEIGHT = 1280;

    final SpriteBatch batch = new SpriteBatch();
    final I18NBundle text;
    final Art art = new Art();
    final Fonts fonts;
    final Themes themes = new Themes();
    final CardPainter cards;
    Styles styles;
    /** Themes stay loaded once used, so Settings can preview them and switching back is instant. */
    private final Map<String, Theme> loaded = new HashMap<>();

    private Theme theme;

    Ui(String themeId) {
        // TeaVM lacks java.text.MessageFormat, which the bundle's default formatter needs.
        I18NBundle.setSimpleFormatter(true);
        text = I18NBundle.createBundle(Gdx.files.internal("i18n/strings"));
        fonts = new Fonts(text);
        cards = new CardPainter(art, fonts, text);
        useTheme(themes.exists(themeId) ? themeId : Themes.DEFAULT);
    }

    String text(String key, Object... args) {
        return text.format(key, args);
    }

    String language() {
        return text.getLocale().getLanguage();
    }

    String themeId() {
        return theme.id;
    }

    Theme theme(String id) {
        return loaded.computeIfAbsent(id, themes::load);
    }

    void useTheme(String id) {
        theme = theme(id);
        cards.use(theme, language());
    }

    String themeName(String id) {
        return id.equals(Theme.CLASSIC)
                ? text("theme.classic")
                : themes.data(id).name().in(language());
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
        for (Theme theme : loaded.values()) {
            theme.dispose();
        }
    }
}
