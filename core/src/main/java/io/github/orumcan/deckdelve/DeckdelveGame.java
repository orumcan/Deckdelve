package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import io.github.orumcan.deckdelve.logic.GameState;
import io.github.orumcan.deckdelve.logic.Rules;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;

public class DeckdelveGame extends Game {
    private Ui ui;
    private Profile profile;

    @Override
    public void create() {
        ui = new Ui();
        profile = new Profile();
        ui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        setScreen(new TitleScreen(this, ui));
    }

    void showTitle() {
        later(() -> setScreen(new TitleScreen(this, ui)));
    }

    void showRules() {
        later(() ->
                setScreen(new PageScreen(this, ui, "rules.title", () -> HowToPlay.content(ui, this::startTutorial))));
    }

    void showCredits() {
        later(() -> setScreen(new PageScreen(this, ui, "credits.title", () -> Credits.content(ui))));
    }

    void startRun() {
        GameState run = Rules.start(Seed.of(MathUtils.random.nextLong()), RulesConfig.OFFICIAL)
                .state();
        later(() -> setScreen(new TableScreen(this, ui, run)));
    }

    boolean tutorialOffered() {
        return profile.tutorialOffered();
    }

    void startTutorial() {
        profile.markTutorialOffered();
        GameState run = Rules.start(Tutorial.SEED, RulesConfig.OFFICIAL).state();
        later(() -> setScreen(new TableScreen(this, ui, run, new Tutorial())));
    }

    void skipTutorial() {
        profile.markTutorialOffered();
        startRun();
    }

    /** Buttons call these from their listeners, so the old screen goes only after the stage finishes the click. */
    private static void later(Runnable action) {
        Gdx.app.postRunnable(action);
    }

    @Override
    public void setScreen(Screen screen) {
        Screen previous = getScreen();
        super.setScreen(screen);
        if (previous != null) {
            previous.dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        // A minimized desktop window reports a size of 0.
        if (width <= 0 || height <= 0) {
            return;
        }
        ui.resize(width, height);
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        ui.dispose();
    }
}
