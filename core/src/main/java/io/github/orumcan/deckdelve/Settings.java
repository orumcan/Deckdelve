package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Suit;
import java.util.List;
import java.util.function.Consumer;

final class Settings {
    /** Every tile shows the same card, so the themes compare like for like. */
    private static final Card PREVIEW = new Card(Suit.CLUBS, Card.ACE);

    private static final float TILE_WIDTH = 300;
    private static final float TILE_PAD = 24;

    private Settings() {}

    static Table content(Ui ui, Consumer<String> useTheme) {
        Table page = new Table();
        Pages.section(page, ui, "settings.cards.title");
        Pages.paragraph(page, ui, "settings.cards.body");
        page.add(themes(ui, useTheme)).padTop(32).row();
        return page;
    }

    /** One tile per theme, two to a row. Picking one applies it at once. */
    private static Table themes(Ui ui, Consumer<String> useTheme) {
        Table grid = new Table();
        grid.defaults().width(TILE_WIDTH).space(24);
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        List<String> ids = ui.themes.ids();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            TextButton tile = tile(ui, id);
            tile.setChecked(id.equals(ui.themeId()));
            group.add(tile);
            StageScreen.onChange(tile, () -> {
                if (tile.isChecked()) {
                    useTheme.accept(id);
                }
            });
            grid.add(tile);
            if (i % 2 == 1) {
                grid.row();
            }
        }
        return grid;
    }

    private static TextButton tile(Ui ui, String id) {
        TextButton tile = new TextButton(ui.themeName(id), ui.styles.choice);
        // Only a tap applies a theme. Setting the tiles up, and the group unchecking the old one, stay silent.
        tile.setProgrammaticChangeEvents(false);
        tile.getLabel().setWrap(true);
        // TextButton keeps its label in its only cell; the card goes above it.
        tile.clearChildren();
        tile.pad(TILE_PAD, TILE_PAD, TILE_PAD + Surfaces.DEPTH, TILE_PAD);
        tile.add(CardActor.face(ui.cards, PREVIEW, ui.theme(id)))
                .size(CardActor.WIDTH, CardActor.HEIGHT)
                .row();
        tile.add(tile.getLabel()).width(TILE_WIDTH - 2 * TILE_PAD).padTop(16);
        return tile;
    }
}
