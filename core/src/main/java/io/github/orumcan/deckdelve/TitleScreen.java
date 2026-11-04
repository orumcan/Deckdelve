package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Suit;

final class TitleScreen extends StageScreen {
    private static final String RULES_URL = "http://www.stfj.net/art/2011/Scoundrel.pdf";
    private static final float FAN_WIDTH = CardActor.WIDTH * 2.6f;

    private final DeckdelveGame game;

    TitleScreen(DeckdelveGame game, Ui ui) {
        super(ui);
        this.game = game;
    }

    @Override
    void build(Table root) {
        root.add(fan()).size(FAN_WIDTH, CardActor.HEIGHT * 1.2f).row();
        root.add(new Label(ui.text("title.name"), ui.styles.title)).padTop(48).row();
        root.add(new Label(ui.text("title.author"), ui.styles.muted)).row();
        root.add(new Label(ui.text("title.credit"), ui.styles.small)).padTop(28).row();
        Label rules = new Label(ui.text("title.rules"), ui.styles.link);
        rules.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI(RULES_URL);
            }
        });
        root.add(rules).padTop(12).row();
        root.add(button(ui.text("title.play"), ui.styles.primary, game::startRun))
                .size(360, 100)
                .padTop(96);
    }

    /** A weapon, a potion and a monster, fanned out like a hand. */
    private Group fan() {
        Card[] cards = {new Card(Suit.DIAMONDS, 10), new Card(Suit.HEARTS, 7), new Card(Suit.SPADES, Card.ACE)};
        float[] angles = {14, -14, 0};
        Group fan = new Group();
        for (int i = 0; i < cards.length; i++) {
            Group holder = new Group();
            holder.setBounds((FAN_WIDTH - CardActor.WIDTH) / 2, 0, CardActor.WIDTH, CardActor.HEIGHT);
            holder.setOrigin(CardActor.WIDTH / 2, -CardActor.HEIGHT * 0.6f);
            holder.setRotation(angles[i]);
            holder.addActor(CardActor.face(ui.cards, cards[i]));
            fan.addActor(holder);
        }
        return fan;
    }

    @Override
    boolean onKey(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            game.startRun();
            return true;
        }
        if (keycode == Input.Keys.BACK) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }
}
