package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import io.github.orumcan.deckdelve.logic.Suit;

final class HealthBar extends Actor {
    private static final float GAP = 14;
    /** Seconds for the shaded part of a preview to fade out and back in. */
    private static final float PULSE = 0.9f;
    /** Seconds from one beat of the broken heart to the next. */
    private static final float BEAT = 0.9f;

    private final Ui ui;
    private final int health;
    private final int max;
    /** With animation off, a preview's shaded part holds still. */
    private final boolean pulsing;
    /** The health drawn, which runs from the old value to the current one while an animation plays. */
    private float shown;
    /**
     * The health a card or choice would leave, shown shaded; the same as health when nothing is previewed. A preview
     * that leaves none is fatal: the label says so, and the heart breaks.
     */
    private int after;
    /** While a change runs, and a moment after, the label shows it as a sum, such as 16 − 3 = 13. */
    private int sumFrom;

    private int sumTo;
    /** How many changes are running or still showing, as the next can start while the last one still shows. */
    private int changes;

    /** The label, kept with what it was written for, so it's only formatted again when that changes. */
    private String label;

    private String labelKey;
    private int labelFirst;
    private int labelSecond;

    private float time;
    private final GlyphLayout layout = new GlyphLayout();

    HealthBar(Ui ui, int health, int max, boolean pulsing) {
        this.ui = ui;
        this.health = health;
        this.max = max;
        this.pulsing = pulsing;
        shown = health;
        after = health;
    }

    void preview(int after) {
        this.after = after;
    }

    void clearPreview() {
        preview(health);
    }

    /** Starts the bar at an earlier health, for an animation to bring it to the current one. */
    void showFrom(int earlier) {
        shown = earlier;
    }

    /** Runs the bar from one health to another after a delay, with the sum in its label until a moment after. */
    void change(int from, int to, float delay, float duration, float hold) {
        addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.run(() -> {
                    sumFrom = from;
                    sumTo = to;
                    changes++;
                }),
                new TemporalAction(duration, Interpolation.smooth) {
                    @Override
                    protected void update(float percent) {
                        shown = from + (to - from) * percent;
                    }
                },
                Actions.delay(hold),
                Actions.run(() -> changes--)));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float size = getHeight();
        // A preview waits for an animation to finish, so it always starts from the current health.
        boolean previewing = after != health && shown == health;
        boolean fatal = previewing && after <= 0;
        float heart = fatal && pulsing ? size * (1 + 0.14f * beat()) : size;
        float inset = (size - heart) / 2;
        batch.setColor(Palette.HEALTH.r, Palette.HEALTH.g, Palette.HEALTH.b, parentAlpha);
        batch.draw(fatal ? ui.art.brokenHeart : ui.art.suit(Suit.HEARTS), getX() + inset, getY() + inset, heart, heart);

        float barX = getX() + size + GAP;
        float barWidth = getWidth() - size - GAP;
        batch.setColor(0, 0, 0, 0.3f * parentAlpha);
        ui.art.rounded.draw(batch, barX, getY(), barWidth, size);
        if (previewing) {
            // What the preview would take away or add is drawn faded, behind the rest, and gently pulses.
            float faded = pulsing ? 0.35f + 0.25f * MathUtils.cosDeg(time / PULSE * 360) : 0.45f;
            fill(batch, barX, barWidth, Math.max(health, after), faded * parentAlpha);
            fill(batch, barX, barWidth, Math.min(health, after), parentAlpha);
        } else {
            fill(batch, barX, barWidth, shown, parentAlpha);
        }
        batch.setColor(Color.WHITE);

        BitmapFont font = ui.fonts.body;
        String text = label(previewing);
        layout.setText(font, text);
        font.setColor(Palette.TEXT.r, Palette.TEXT.g, Palette.TEXT.b, parentAlpha);
        font.draw(batch, text, barX + (barWidth - layout.width) / 2, getY() + (size + font.getData().capHeight) / 2);
    }

    private String label(boolean previewing) {
        if (previewing && after <= 0) {
            return sum("table.healthFatal", health, after);
        }
        if (previewing) {
            return sum(after < health ? "table.healthLoss" : "table.healthGain", health, after);
        }
        if (changes > 0) {
            return sum(sumTo < sumFrom ? "table.healthLoss" : "table.healthGain", sumFrom, sumTo);
        }
        return label("table.health", Math.round(shown), max, max);
    }

    /** A change from one health to another, written as a sum by the text with this key. */
    private String sum(String key, int from, int to) {
        return label(key, from, Math.abs(to - from), to);
    }

    private String label(String key, int first, int second, int third) {
        if (!key.equals(labelKey) || first != labelFirst || second != labelSecond) {
            labelKey = key;
            labelFirst = first;
            labelSecond = second;
            label = ui.text(key, Ui.number(first), second, Ui.number(third));
        }
        return label;
    }

    /** A heartbeat from 0 to 1: two quick swells, the second weaker, and a rest. */
    private float beat() {
        float phase = time % BEAT / BEAT;
        return Math.max(swell(phase), 0.6f * swell(phase - 0.26f));
    }

    private static float swell(float phase) {
        float t = phase / 0.2f;
        return t < 0 || t > 1 ? 0 : MathUtils.sin(t * MathUtils.PI);
    }

    private void fill(Batch batch, float barX, float barWidth, float value, float alpha) {
        float size = getHeight();
        float filled = MathUtils.clamp(value / max, 0, 1) * barWidth;
        if (filled <= 0) {
            return;
        }
        float fillWidth = Math.max(filled, size);
        // A lighter rim along the top edge, like the buttons.
        batch.setColor(Palette.HEALTH_RIM.r, Palette.HEALTH_RIM.g, Palette.HEALTH_RIM.b, alpha);
        ui.art.rounded.draw(batch, barX, getY(), fillWidth, size);
        batch.setColor(Palette.HEALTH.r, Palette.HEALTH.g, Palette.HEALTH.b, alpha);
        ui.art.rounded.draw(batch, barX, getY(), fillWidth, size - 3);
    }
}
