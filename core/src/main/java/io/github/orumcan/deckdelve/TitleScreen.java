package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Suit;

final class TitleScreen extends StageScreen {
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
        root.add(button(ui.text("title.play"), ui.styles.primary, this::play))
                .size(360, 100)
                .padTop(72)
                .row();
        Table more = new Table();
        more.defaults().size(240, 84).space(24);
        more.add(button(ui.text("title.rules"), ui.styles.secondary, game::showRules));
        more.add(button(ui.text("title.tutorial"), ui.styles.secondary, game::startTutorial))
                .row();
        more.add(button(ui.text("title.settings"), ui.styles.secondary, game::showSettings));
        more.add(button(ui.text("title.credits"), ui.styles.secondary, game::showCredits));
        root.add(more).padTop(24);
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

    private void play() {
        if (game.tutorialOffered()) {
            game.startRun();
        } else {
            offerTutorial();
        }
    }

    /** Asked once, the first time someone plays. */
    private void offerTutorial() {
        Table panel = panel();
        panel.add(heading(ui.text("offer.title"))).growX().row();
        Label body = new Label(ui.text("offer.body"), ui.styles.body);
        body.setWrap(true);
        body.setAlignment(Align.center);
        panel.add(body).growX().padTop(12).row();
        panel.add(button(ui.text("offer.tutorial"), ui.styles.primary, game::startTutorial))
                .growX()
                .height(92)
                .padTop(32)
                .row();
        panel.add(button(ui.text("offer.skip"), ui.styles.secondary, game::skipTutorial))
                .growX()
                .height(84)
                .padTop(16);
        open(panel, this::offerTutorial, true, keycode -> {
            if (isBack(keycode)) {
                close();
                return true;
            }
            return false;
        });
    }

    @Override
    boolean onKey(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            play();
            return true;
        }
        if (keycode == Input.Keys.BACK) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }
}
