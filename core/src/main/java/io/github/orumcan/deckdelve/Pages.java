package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Align;
import io.github.orumcan.deckdelve.logic.Card;

/** Scrolling pages of text, on a screen of their own or over the table: How to Play and Credits. */
final class Pages {
    static final float PAD = 28;
    static final float TEXT_WIDTH = Ui.WIDTH - 2 * PAD;
    private static final float KEY_STEP = 120;

    private Pages() {}

    /** A back button and the title on top, the scrolling content below. */
    static Table frame(Ui ui, String title, ScrollPane scroll, Runnable back) {
        Table header = new Table();
        header.add(StageScreen.button(ui.text("page.back"), ui.styles.secondary, back))
                .size(150, 64);
        // The right padding matches the back button, so the title sits in the middle of the page.
        header.add(new Label(title, ui.styles.heading)).expandX().padRight(150);
        Table frame = new Table();
        frame.add(header).growX().pad(PAD, PAD, 16, PAD).row();
        frame.add(scroll).grow();
        return frame;
    }

    static ScrollPane scroller(Ui ui, Table content) {
        content.top().pad(8, PAD, PAD * 2, PAD);
        // ScrollPane's own wheel step is a fifth of the page, which throws a trackpad's many small steps to the end.
        ScrollPane scroll = new ScrollPane(content, ui.styles.scroll) {
            @Override
            protected float getMouseWheelY() {
                return wheelPixels() * getStage().getHeight() / Gdx.graphics.getHeight();
            }
        };
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        // The wheel moves the page at once, as a finger on a trackpad expects. Only the keys glide.
        scroll.addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                scroll.updateVisualScroll();
                return true;
            }
        });
        return scroll;
    }

    /**
     * The screen pixels a page moves for each unit of wheel scrolling. What a unit is depends on the platform: the web
     * launcher reports pixels, macOS a tenth of the distance a trackpad moves, and the others one notch of a wheel.
     */
    private static float wheelPixels() {
        if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
            return 1;
        }
        return UIUtils.isMac ? 16 : 48;
    }

    /** Scrolls with the arrow and page keys, and returns whether the key was one of them. */
    static boolean scrollByKey(ScrollPane scroll, int keycode) {
        float step = switch (keycode) {
            case Input.Keys.UP -> -KEY_STEP;
            case Input.Keys.DOWN -> KEY_STEP;
            case Input.Keys.PAGE_UP -> -scroll.getScrollHeight();
            case Input.Keys.PAGE_DOWN -> scroll.getScrollHeight();
            default -> 0;
        };
        if (step == 0) {
            return false;
        }
        scroll.setScrollY(scroll.getScrollY() + step);
        return true;
    }

    static void section(Table page, Ui ui, String titleKey) {
        page.add(new Label(ui.text(titleKey), ui.styles.section))
                .width(TEXT_WIDTH)
                .padTop(page.getRows() == 0 ? 0 : 40)
                .row();
    }

    static void paragraph(Table page, Ui ui, String key, Object... args) {
        Label label = new Label(ui.text(key, args), ui.styles.body);
        label.setWrap(true);
        page.add(label).width(TEXT_WIDTH).padTop(12).row();
    }

    static void link(Table page, Ui ui, String key, String url) {
        Label label = new Label(ui.text(key), ui.styles.accent);
        label.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI(url);
            }
        });
        page.add(label).left().padTop(16).row();
    }

    /** A row of full-size cards, each with a short caption underneath. */
    static Table cards(Ui ui, Card[] cards, String[] captionKeys) {
        Table row = new Table();
        row.defaults().space(20);
        for (Card card : cards) {
            row.add(CardActor.face(ui.cards, card)).size(CardActor.WIDTH, CardActor.HEIGHT);
        }
        row.row();
        for (String key : captionKeys) {
            row.add(caption(ui, key)).width(CardActor.WIDTH).spaceTop(10);
        }
        return row;
    }

    static Label caption(Ui ui, String key) {
        Label caption = new Label(ui.text(key), ui.styles.small);
        caption.setWrap(true);
        caption.setAlignment(Align.center);
        return caption;
    }
}
