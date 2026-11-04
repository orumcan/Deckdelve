package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Suit;

/** The rules in our own words, with examples laid out in real cards. */
final class HowToPlay {
    static final String ORIGINAL_RULES = "http://www.stfj.net/art/2011/Scoundrel.pdf";

    private HowToPlay() {}

    /** {@code tutorial} adds a button that starts the tutorial; pass null where starting one would end a run. */
    static Table content(Ui ui, Runnable tutorial) {
        Table page = new Table();
        Pages.section(page, ui, "rules.goal.title");
        Pages.paragraph(page, ui, "rules.goal.body");

        Pages.section(page, ui, "rules.cards.title");
        Pages.paragraph(page, ui, "rules.cards.body");
        example(
                page,
                Pages.cards(
                        ui,
                        new Card[] {
                            new Card(Suit.CLUBS, 9),
                            new Card(Suit.SPADES, Card.QUEEN),
                            new Card(Suit.DIAMONDS, 5),
                            new Card(Suit.HEARTS, 6)
                        },
                        new String[] {
                            "rules.cards.monster", "rules.cards.monster", "rules.cards.weapon", "rules.cards.potion"
                        }));

        Pages.section(page, ui, "rules.rooms.title");
        Pages.paragraph(page, ui, "rules.rooms.body");
        example(
                page,
                Pages.cards(
                        ui,
                        new Card[] {
                            new Card(Suit.CLUBS, 8),
                            new Card(Suit.DIAMONDS, 5),
                            new Card(Suit.HEARTS, 3),
                            new Card(Suit.SPADES, Card.JACK)
                        },
                        new String[] {
                            "rules.rooms.second", "rules.rooms.first", "rules.rooms.third", "rules.rooms.stays"
                        }));
        Pages.paragraph(page, ui, "rules.rooms.example");
        Pages.paragraph(page, ui, "rules.rooms.avoid");
        Pages.paragraph(page, ui, "rules.rooms.last");

        Pages.section(page, ui, "rules.fight.title");
        Pages.paragraph(page, ui, "rules.fight.body");
        Pages.paragraph(page, ui, "rules.fight.equip");

        Pages.section(page, ui, "rules.weapon.title");
        Pages.paragraph(page, ui, "rules.weapon.body");
        example(page, weaponExample(ui));
        Pages.paragraph(page, ui, "rules.weapon.example");

        Pages.section(page, ui, "rules.potions.title");
        Pages.paragraph(page, ui, "rules.potions.body");
        example(page, Pages.cards(ui, new Card[] {new Card(Suit.HEARTS, 7), new Card(Suit.HEARTS, 4)}, new String[] {
            "rules.potions.heals", "rules.potions.wasted"
        }));
        Pages.paragraph(page, ui, "rules.potions.example");

        Pages.section(page, ui, "rules.score.title");
        Pages.paragraph(page, ui, "rules.score.body");
        Pages.paragraph(page, ui, "rules.score.death");

        Pages.section(page, ui, "rules.original.title");
        Pages.paragraph(page, ui, "rules.original.body");
        Pages.link(page, ui, "rules.original.link", ORIGINAL_RULES);
        if (tutorial != null) {
            page.add(StageScreen.button(ui.text("rules.tutorial"), ui.styles.primary, tutorial))
                    .size(360, 92)
                    .padTop(48)
                    .row();
        }
        return page;
    }

    private static void example(Table page, Table cards) {
        page.add(cards).padTop(28).row();
    }

    /** The rulebook's own case: a 5 whose last kill was a queen, facing a 6. */
    private static Table weaponExample(Ui ui) {
        float stackWidth = CardActor.WIDTH * 1.45f;
        Group stack = new Group();
        stack.addActor(CardActor.face(ui.cards, new Card(Suit.DIAMONDS, 5)));
        CardActor queen = CardActor.face(ui.cards, new Card(Suit.SPADES, Card.QUEEN));
        queen.setX(stackWidth - CardActor.WIDTH);
        stack.addActor(queen);

        Table row = new Table();
        row.add(stack).size(stackWidth, CardActor.HEIGHT);
        row.add(CardActor.face(ui.cards, new Card(Suit.CLUBS, 6)))
                .size(CardActor.WIDTH, CardActor.HEIGHT)
                .padLeft(48);
        row.row();
        row.add(Pages.caption(ui, "rules.weapon.stack")).width(stackWidth).padTop(10);
        row.add(Pages.caption(ui, "rules.weapon.next"))
                .width(CardActor.WIDTH)
                .padLeft(48)
                .padTop(10);
        return row;
    }
}
