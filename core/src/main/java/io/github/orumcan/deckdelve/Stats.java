package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.orumcan.deckdelve.logic.Card;
import java.util.Optional;

/** The numbers worth keeping across runs, and the monster that has ended the most of them, shown as a card. */
final class Stats {
    private Stats() {}

    static Table content(Ui ui, Profile profile) {
        Table page = new Table();
        Table numbers = new Table();
        numbers.defaults().padTop(16);
        row(numbers, ui, "stats.runs", Integer.toString(profile.runs()));
        row(numbers, ui, "stats.wins", Integer.toString(profile.career().wins()));
        row(numbers, ui, "stats.streak", Integer.toString(profile.career().streak()));
        row(numbers, ui, "stats.best", profile.bestScore().map(String::valueOf).orElse(ui.text("stats.none")));
        page.add(numbers).width(Pages.TEXT_WIDTH).row();

        Pages.section(page, ui, "stats.nemesis.title");
        Optional<Card> nemesis = profile.nemesis();
        if (nemesis.isPresent()) {
            page.add(CardActor.face(ui.cards, nemesis.get()))
                    .size(CardActor.WIDTH, CardActor.HEIGHT)
                    .padTop(20)
                    .row();
            page.add(new Label(ui.text("stats.nemesis.body", profile.deathsTo(nemesis.get())), ui.styles.body))
                    .padTop(12)
                    .row();
        } else {
            Pages.paragraph(page, ui, "stats.nemesis.none");
        }
        Label counting = new Label(ui.text("stats.counting"), ui.styles.small);
        counting.setWrap(true);
        page.add(counting).width(Pages.TEXT_WIDTH).padTop(40).row();
        return page;
    }

    private static void row(Table numbers, Ui ui, String key, String value) {
        numbers.add(new Label(ui.text(key), ui.styles.body)).left().expandX();
        numbers.add(new Label(value, ui.styles.strong)).right().row();
    }
}
