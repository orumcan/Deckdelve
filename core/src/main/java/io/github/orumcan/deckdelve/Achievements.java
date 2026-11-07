package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.orumcan.deckdelve.logic.Achievement;
import java.util.Set;

/** Every achievement on a flat tile, two to a row: the unlocked ones in gold, the rest dimmed. */
final class Achievements {
    private static final float GAP = 20;
    private static final float TILE_WIDTH = (Pages.TEXT_WIDTH - GAP) / 2;
    private static final float TILE_PAD = 20;

    private Achievements() {}

    static Table content(Ui ui, Profile profile) {
        Set<Achievement> unlocked = profile.achievements();
        Table page = new Table();
        Label count =
                new Label(ui.text("achievements.count", unlocked.size(), Achievement.values().length), ui.styles.muted);
        page.add(count).row();
        Table grid = new Table();
        grid.defaults().width(TILE_WIDTH).space(GAP).fill();
        Achievement[] all = Achievement.values();
        for (int i = 0; i < all.length; i++) {
            grid.add(tile(ui, all[i], unlocked.contains(all[i])));
            if (i % 2 == 1) {
                grid.row();
            }
        }
        page.add(grid).padTop(24).row();
        return page;
    }

    private static Table tile(Ui ui, Achievement achievement, boolean unlocked) {
        Table tile = new Table();
        tile.setBackground(unlocked ? ui.styles.tip : ui.styles.panel);
        tile.pad(TILE_PAD);
        tile.top().left();
        Label name =
                new Label(ui.text(Ui.key("achievement", achievement)), unlocked ? ui.styles.section : ui.styles.muted);
        name.setWrap(true);
        tile.add(name).growX().row();
        Label body = new Label(ui.text(Ui.key("achievement", achievement) + ".body"), ui.styles.small);
        body.setWrap(true);
        tile.add(body).growX().padTop(8);
        if (!unlocked) {
            tile.getColor().a = 0.6f;
        }
        return tile;
    }
}
