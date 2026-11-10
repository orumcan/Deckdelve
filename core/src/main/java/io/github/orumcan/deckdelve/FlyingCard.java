package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import io.github.orumcan.deckdelve.logic.Card;

/** A card on its way from one place on the table to another, drawn above the table while an animation plays. */
final class FlyingCard extends Actor {
    private final CardPainter painter;
    private final Card card;
    private boolean faceUp;
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 squashed = new Matrix4();

    FlyingCard(CardPainter painter, Card card, boolean faceUp, Rectangle from) {
        this.painter = painter;
        this.card = card;
        this.faceUp = faceUp;
        setBounds(from.x, from.y, from.width, from.height);
    }

    /** Squeezes the card to its edge and opens it out showing its other side. */
    Action turnOver(float duration) {
        return Actions.sequence(
                Actions.scaleTo(0, 1, duration / 2, Interpolation.sineIn),
                Actions.run(() -> faceUp = !faceUp),
                Actions.scaleTo(1, 1, duration / 2, Interpolation.sineOut));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float alpha = getColor().a * parentAlpha;
        // The painter sizes a card by its width alone, so a turn squeezes it through the batch's transform instead.
        float middle = getX() + getWidth() / 2;
        saved.set(batch.getTransformMatrix());
        squashed.set(saved).translate(middle, 0, 0).scale(getScaleX(), 1, 1).translate(-middle, 0, 0);
        batch.setTransformMatrix(squashed);
        if (faceUp) {
            painter.face(batch, card, getX(), getY(), getWidth(), alpha);
        } else {
            painter.back(batch, getX(), getY(), getWidth(), alpha);
        }
        batch.setTransformMatrix(saved);
    }
}
