package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.function.Supplier;

/** How to Play or Credits as a screen of its own, opened from the title screen. */
final class PageScreen extends StageScreen {
    private final DeckdelveGame game;
    private final String titleKey;
    private final Supplier<Table> content;
    private ScrollPane scroll;

    PageScreen(DeckdelveGame game, Ui ui, String titleKey, Supplier<Table> content) {
        super(ui);
        this.game = game;
        this.titleKey = titleKey;
        this.content = content;
    }

    @Override
    void build(Table root) {
        float scrolled = scroll == null ? 0 : scroll.getScrollY();
        scroll = Pages.scroller(ui, content.get());
        root.add(Pages.frame(ui, ui.text(titleKey), scroll, game::showTitle))
                .width(Ui.WIDTH)
                .growY();
        stage.setScrollFocus(scroll);
        // A resize rebuilds the page, so it returns to where the reader was.
        root.validate();
        scroll.setScrollY(scrolled);
        scroll.updateVisualScroll();
    }

    @Override
    boolean onKey(int keycode) {
        if (isBack(keycode)) {
            game.showTitle();
            return true;
        }
        return Pages.scrollByKey(scroll, keycode);
    }
}
