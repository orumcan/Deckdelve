package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.orumcan.deckdelve.logic.Card;

final class CardActor extends Actor {
    static final float WIDTH = 150;
    static final float HEIGHT = WIDTH * CardPainter.RATIO;

    private enum Side {
        FACE,
        BACK,
        SLOT
    }

    private final CardPainter painter;
    private final Card card;
    private final Side side;
    private ClickListener clicks;

    private CardActor(CardPainter painter, Card card, Side side, float width) {
        this.painter = painter;
        this.card = card;
        this.side = side;
        setSize(width, width * CardPainter.RATIO);
    }

    static CardActor face(CardPainter painter, Card card) {
        return new CardActor(painter, card, Side.FACE, WIDTH);
    }

    static CardActor back(CardPainter painter, float width) {
        return new CardActor(painter, null, Side.BACK, width);
    }

    static CardActor slot(CardPainter painter, float width) {
        return new CardActor(painter, null, Side.SLOT, width);
    }

    CardActor onClick(Runnable action) {
        clicks = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        };
        addListener(clicks);
        return this;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float alpha = getColor().a * parentAlpha;
        switch (side) {
            case FACE -> {
                float lift = clicks != null && clicks.isOver() ? 8 : 0;
                painter.face(batch, card, getX(), getY() + lift, getWidth(), alpha);
            }
            case BACK -> painter.back(batch, getX(), getY(), getWidth(), alpha);
            case SLOT -> painter.slot(batch, getX(), getY(), getWidth(), alpha);
        }
    }
}
