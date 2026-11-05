package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** What the game remembers between sessions. */
final class Profile {
    private static final String TUTORIAL_OFFERED = "tutorialOffered";
    private static final String THEME = "theme";

    private final Preferences preferences = Gdx.app.getPreferences("io.github.orumcan.deckdelve");

    boolean tutorialOffered() {
        return preferences.getBoolean(TUTORIAL_OFFERED, false);
    }

    void markTutorialOffered() {
        preferences.putBoolean(TUTORIAL_OFFERED, true);
        preferences.flush();
    }

    String theme() {
        return preferences.getString(THEME, Themes.DEFAULT);
    }

    void theme(String id) {
        preferences.putString(THEME, id);
        preferences.flush();
    }
}
