package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

final class Credits {
    private static final String SOURCE = "https://github.com/orumcan/scoundrel";

    private Credits() {}

    static Table content(Ui ui) {
        Table page = new Table();
        page.add(new Label(ui.text("title.name"), ui.styles.title)).row();
        page.add(new Label(ui.text("title.author"), ui.styles.muted)).row();

        Pages.section(page, ui, "credits.game.title");
        Pages.paragraph(page, ui, "credits.game.body");
        Pages.link(page, ui, "rules.original.link", HowToPlay.ORIGINAL_RULES);

        Pages.section(page, ui, "credits.font.title");
        Pages.paragraph(page, ui, "credits.font.body");

        Pages.section(page, ui, "credits.software.title");
        Pages.paragraph(page, ui, "credits.software.body");
        // FreeType's license asks binary releases to show this line in their documentation.
        Pages.paragraph(page, ui, "credits.freetype");

        Pages.section(page, ui, "credits.license.title");
        Pages.paragraph(page, ui, "credits.license.body");
        Pages.link(page, ui, "credits.source", SOURCE);
        return page;
    }
}
