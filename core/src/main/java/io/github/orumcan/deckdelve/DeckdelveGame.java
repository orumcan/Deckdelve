package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.Seed;
import java.util.Optional;

public class DeckdelveGame extends Game {
    private Ui ui;
    private Profile profile;

    @Override
    public void create() {
        profile = new Profile();
        ui = new Ui(profile.theme());
        ui.sounds.volume(profile.volume() / 100f);
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

    void showSettings() {
        later(() -> setScreen(
                new PageScreen(this, ui, "settings.title", () -> Settings.content(ui, this::useTheme, profile))));
    }

    void showAchievements() {
        later(() -> setScreen(new PageScreen(this, ui, "achievements.title", () -> Achievements.content(ui, profile))));
    }

    void showStats() {
        later(() -> setScreen(new PageScreen(this, ui, "stats.title", () -> Stats.content(ui, profile))));
    }

    Profile profile() {
        return profile;
    }

    void useTheme(String id) {
        ui.useTheme(id);
        profile.theme(id);
    }

    /** Continues the saved run as it was left, or starts a new one when there's none. */
    void play() {
        Optional<SavedRun> saved = profile.savedRun();
        if (saved.isPresent()) {
            later(() -> setScreen(new TableScreen(this, ui, saved.get())));
        } else {
            startRun();
        }
    }

    boolean hasSavedRun() {
        return profile.savedRun().isPresent();
    }

    /** A new run with the rules picked in Settings, saved from its first room. */
    void startRun() {
        SavedRun run = SavedRun.start(Seed.of(MathUtils.random.nextLong()), profile.rules());
        profile.save(run);
        later(() -> setScreen(new TableScreen(this, ui, run)));
    }

    boolean tutorialOffered() {
        return profile.tutorialOffered();
    }

    void startTutorial() {
        profile.markTutorialOffered();
        SavedRun run = SavedRun.start(Tutorial.SEED, RulesConfig.OFFICIAL);
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
