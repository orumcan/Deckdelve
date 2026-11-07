package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import io.github.orumcan.deckdelve.logic.Suit;

final class HealthBar extends Actor {
    private static final float GAP = 14;

    private final Ui ui;
    private final int health;
    private final int max;
    private final String label;
    private final GlyphLayout layout = new GlyphLayout();

    HealthBar(Ui ui, int health, int max) {
        this.ui = ui;
        this.health = health;
        this.max = max;
        label = ui.text("table.health", health, max);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float size = getHeight();
        batch.setColor(Palette.HEALTH.r, Palette.HEALTH.g, Palette.HEALTH.b, parentAlpha);
        batch.draw(ui.art.suit(Suit.HEARTS), getX(), getY(), size, size);

        float barX = getX() + size + GAP;
        float barWidth = getWidth() - size - GAP;
        batch.setColor(0, 0, 0, 0.3f * parentAlpha);
        ui.art.rounded.draw(batch, barX, getY(), barWidth, size);
        float filled = MathUtils.clamp(health / (float) max, 0, 1) * barWidth;
        if (filled > 0) {
            float fillWidth = Math.max(filled, size);
            // A lighter rim along the top edge, like the buttons.
            batch.setColor(Palette.HEALTH_RIM.r, Palette.HEALTH_RIM.g, Palette.HEALTH_RIM.b, parentAlpha);
            ui.art.rounded.draw(batch, barX, getY(), fillWidth, size);
            batch.setColor(Palette.HEALTH.r, Palette.HEALTH.g, Palette.HEALTH.b, parentAlpha);
            ui.art.rounded.draw(batch, barX, getY(), fillWidth, size - 3);
        }
        batch.setColor(Color.WHITE);

        BitmapFont font = ui.fonts.body;
        layout.setText(font, label);
        font.setColor(Palette.TEXT.r, Palette.TEXT.g, Palette.TEXT.b, parentAlpha);
        font.draw(batch, label, barX + (barWidth - layout.width) / 2, getY() + (size + font.getData().capHeight) / 2);
    }
}
