package io.github.orumcan.deckdelve;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** A face-down pile that looks thicker the more cards it holds, with its count underneath. */
final class Pile extends Table {
    static final float CARD_WIDTH = 112;

    private static final float CARD_HEIGHT = CARD_WIDTH * CardPainter.RATIO;
    /** How far each layer of a thicker pile sits from the one above it. */
    private static final float LAYER = 4;

    private final Ui ui;
    private final String key;
    private final Group stack = new Group();
    private final Label label;
    private int count;

    Pile(Ui ui, String key, int count) {
        this.ui = ui;
        this.key = key;
        label = new Label("", ui.styles.small);
        add(stack).size(CARD_WIDTH + 2 * LAYER, CARD_HEIGHT + 2 * LAYER).row();
        add(label).padTop(10);
        count(count);
    }

    /** Shows a new count, which an animation changes card by card. */
    void count(int count) {
        this.count = count;
        stack.clearChildren();
        int layers = count == 0 ? 0 : Math.min(3, 1 + count / 15);
        if (layers == 0) {
            stack.addActor(CardActor.slot(ui.cards, CARD_WIDTH));
        }
        for (int layer = layers - 1; layer >= 0; layer--) {
            CardActor back = CardActor.back(ui.cards, CARD_WIDTH);
            back.setPosition(layer * LAYER, 2 * LAYER - layer * LAYER);
            stack.addActor(back);
        }
        label.setText(ui.text(key, count));
    }

    void addCard() {
        count(count + 1);
    }

    void takeCard() {
        count(count - 1);
    }

    /** Where a card joining or leaving the pile lies: on top, in stage coordinates. */
    Rectangle topCard() {
        Vector2 corner = stack.localToStageCoordinates(new Vector2(0, 2 * LAYER));
        return new Rectangle(corner.x, corner.y, CARD_WIDTH, CARD_HEIGHT);
    }
}
