package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Drawables for things that sit above the table, so buttons and dialogs read as objects instead of as the flat slots
 * cards go in.
 */
final class Surfaces {
    /** How far a button's face sits above its lip, in world units. */
    static final float DEPTH = 6;

    /** How far a pressed button's face sinks toward the table. */
    static final float PRESS = 4;

    private Surfaces() {}

    /** A soft shadow, a darker lip under the face, and a light rim along the face's top edge. */
    static Drawable raised(Art art, Color face, Color lip, boolean pressed) {
        Color rim = face.cpy().lerp(Color.WHITE, 0.3f);
        float sink = pressed ? PRESS : 0;
        return padded(new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                Color tint = batch.getColor();
                float r = tint.r;
                float g = tint.g;
                float b = tint.b;
                float a = tint.a;
                float top = y + height - sink;
                float faceY = y + DEPTH - sink;
                float spread = art.shadowSpread();
                batch.setColor(0, 0, 0, (pressed ? 0.2f : 0.4f) * a);
                art.shadow.draw(batch, x - spread, y - spread - 3, width + 2 * spread, top - y + 2 * spread);
                paint(batch, art.rounded, lip, a, x, y, width, top - y);
                paint(batch, art.rounded, rim, a, x, faceY, width, top - faceY);
                paint(batch, art.rounded, face, a, x, faceY, width, top - faceY - 2);
                batch.setColor(r, g, b, a);
            }
        });
    }

    /** A disabled button: the face lies flat on the table, with the same padding so its label doesn't move. */
    static Drawable flat(Art art, Color color) {
        return padded(new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                Color tint = batch.getColor();
                float r = tint.r;
                float g = tint.g;
                float b = tint.b;
                float a = tint.a;
                paint(batch, art.rounded, color, a, x, y + DEPTH, width, height - DEPTH);
                batch.setColor(r, g, b, a);
            }
        });
    }

    /** A dialog floating over the dimmed table: a wide soft shadow and a slightly lighter border. */
    static Drawable floating(Art art, Color face, Color border) {
        return new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                Color tint = batch.getColor();
                float r = tint.r;
                float g = tint.g;
                float b = tint.b;
                float a = tint.a;
                float spread = art.wideShadowSpread();
                batch.setColor(0, 0, 0, 0.55f * a);
                art.wideShadow.draw(batch, x - spread, y - spread - 10, width + 2 * spread, height + 2 * spread);
                paint(batch, art.rounded, border, a, x, y, width, height);
                paint(batch, art.rounded, face, a, x + 2, y + 2, width - 4, height - 4);
                batch.setColor(r, g, b, a);
            }
        };
    }

    /** Keeps a button's label centred on its face, above the lip. */
    private static Drawable padded(BaseDrawable drawable) {
        drawable.setLeftWidth(20);
        drawable.setRightWidth(20);
        drawable.setTopHeight(10);
        drawable.setBottomHeight(10 + DEPTH);
        drawable.setMinWidth(48);
        drawable.setMinHeight(48);
        return drawable;
    }

    private static void paint(
            Batch batch, NinePatch patch, Color color, float alpha, float x, float y, float width, float height) {
        batch.setColor(color.r, color.g, color.b, color.a * alpha);
        patch.draw(batch, x, y, width, height);
    }
}
