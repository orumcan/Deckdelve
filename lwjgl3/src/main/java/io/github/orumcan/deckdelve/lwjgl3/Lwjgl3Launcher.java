package io.github.orumcan.deckdelve.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.orumcan.deckdelve.DeckdelveGame;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // Handles macOS support and helps on Windows.
        new Lwjgl3Application(new DeckdelveGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Deckdelve");
        configuration.useVsync(true);
        // Vsync doesn't always work on Linux, so the frame rate is also capped at the monitor's refresh rate.
        Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        configuration.setForegroundFPS(display.refreshRate + 1);
        // The table is a portrait layout, so the window opens portrait-shaped at most of the screen's height.
        int height = Math.round(display.height * 0.85f);
        configuration.setWindowedMode(height * 9 / 16, height);
        configuration.setWindowSizeLimits(360, 640, -1, -1);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
