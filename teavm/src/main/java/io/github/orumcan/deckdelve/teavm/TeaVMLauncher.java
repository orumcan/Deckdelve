package io.github.orumcan.deckdelve.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import io.github.orumcan.deckdelve.DeckdelveGame;

/** Launches the web (TeaVM) application. */
public class TeaVMLauncher {
    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        // A width and height of 0 make the canvas fill the browser window.
        config.width = 0;
        config.height = 0;
        new WebApplication(new DeckdelveGame(), config);
    }
}
