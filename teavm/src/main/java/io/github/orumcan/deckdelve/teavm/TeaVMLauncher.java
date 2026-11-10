package io.github.orumcan.deckdelve.teavm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import io.github.orumcan.deckdelve.DeckdelveGame;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLDocument;

/** Launches the web (TeaVM) application. */
public class TeaVMLauncher {
    /** The pixels in a line, for browsers that report a mouse wheel in lines. */
    private static final int LINE = 18;

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
        scrollByDistance();
    }

    /**
     * gdx-teavm counts every wheel event as one notch, whatever distance it carries, so a trackpad's stream of small
     * events would throw a page to its end. This hands the game each event's distance in screen pixels instead, and
     * stops the event before gdx-teavm's own listener on the document counts it too.
     */
    private static void scrollByDistance() {
        Window window = Window.current();
        window.addEventListener(
                "wheel",
                event -> {
                    WheelEvent wheel = (WheelEvent) event;
                    // A pinch arrives as a wheel event with Ctrl held, and zooms the browser's page.
                    if (wheel.getCtrlKey()) {
                        return;
                    }
                    event.stopPropagation();
                    InputProcessor game = Gdx.input == null ? null : Gdx.input.getInputProcessor();
                    if (game != null) {
                        double distance = wheel.getDeltaY();
                        if (wheel.getDeltaMode() == WheelEvent.DOM_DELTA_LINE) {
                            distance *= LINE;
                        } else if (wheel.getDeltaMode() == WheelEvent.DOM_DELTA_PAGE) {
                            distance *= window.getInnerHeight();
                        }
                        game.scrolled(0, (float) (distance * window.getDevicePixelRatio()));
                    }
                },
                true);
    }
}
