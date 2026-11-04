package io.github.orumcan.deckdelve.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import io.github.orumcan.deckdelve.DeckdelveGame;
import org.teavm.jso.dom.html.HTMLDocument;

/** Launches the web (TeaVM) application. */
public class TeaVMLauncher {
    public static void main(String[] args) {
        // gdx-teavm's page has no viewport tag, so phone browsers would lay the game out 980 pixels wide.
        HTMLDocument document = HTMLDocument.current();
        document.getHead().appendChild(document.createElement("meta", meta -> {
            meta.setAttribute("name", "viewport");
            meta.setAttribute("content", "width=device-width, initial-scale=1");
        }));
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        // A width and height of 0 make the canvas fill the browser window.
        config.width = 0;
        config.height = 0;
        // Renders at the screen's real resolution, so text and cards stay sharp on high-density displays.
        config.usePhysicalPixels = true;
        // FreeType runs from a script that has to finish loading before the game creates its fonts.
        config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");
        new WebApplication(new DeckdelveGame(), config);
    }
}
