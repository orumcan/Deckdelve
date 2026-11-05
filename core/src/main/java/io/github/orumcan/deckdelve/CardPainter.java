package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.CardType;
import io.github.orumcan.deckdelve.logic.Suit;

/**
 * Draws cards the way the physical deck looks: rank and suit in two corners, and pips or a framed court in between.
 * A theme puts its icon and the card's name in the frame instead.
 */
final class CardPainter {
    static final float RATIO = 1.4f;

    /** The card width the font sizes were chosen for; other widths scale from it. */
    private static final float DESIGN_WIDTH = 150;

    private static final float LEFT = 0.335f;
    private static final float CENTER = 0.5f;
    private static final float RIGHT = 0.665f;
    private static final float TOP = 0.79f;
    private static final float UPPER = 0.597f;
    private static final float MIDDLE = 0.5f;
    private static final float LOWER = 0.403f;
    private static final float BOTTOM = 0.21f;
    private static final float FRAME_LEFT = 0.22f;
    private static final float FRAME_BOTTOM = 0.17f;
    private static final float FRAME_WIDTH = 0.56f;
    private static final float FRAME_HEIGHT = 0.66f;
    private static final float[][] PIPS = new float[11][];

    static {
        PIPS[2] = new float[] {CENTER, TOP, CENTER, BOTTOM};
        PIPS[3] = new float[] {CENTER, TOP, CENTER, MIDDLE, CENTER, BOTTOM};
        PIPS[4] = new float[] {LEFT, TOP, RIGHT, TOP, LEFT, BOTTOM, RIGHT, BOTTOM};
        PIPS[5] = new float[] {LEFT, TOP, RIGHT, TOP, CENTER, MIDDLE, LEFT, BOTTOM, RIGHT, BOTTOM};
        PIPS[6] = new float[] {LEFT, TOP, RIGHT, TOP, LEFT, MIDDLE, RIGHT, MIDDLE, LEFT, BOTTOM, RIGHT, BOTTOM};
        PIPS[7] = new float[] {
            LEFT, TOP, RIGHT, TOP, CENTER, (TOP + MIDDLE) / 2, LEFT, MIDDLE, RIGHT, MIDDLE, LEFT, BOTTOM, RIGHT, BOTTOM
        };
        PIPS[8] = new float[] {
            LEFT,
            TOP,
            RIGHT,
            TOP,
            CENTER,
            (TOP + MIDDLE) / 2,
            LEFT,
            MIDDLE,
            RIGHT,
            MIDDLE,
            CENTER,
            (MIDDLE + BOTTOM) / 2,
            LEFT,
            BOTTOM,
            RIGHT,
            BOTTOM
        };
        PIPS[9] = new float[] {
            LEFT, TOP, RIGHT, TOP, LEFT, UPPER, RIGHT, UPPER, CENTER, MIDDLE, LEFT, LOWER, RIGHT, LOWER, LEFT, BOTTOM,
            RIGHT, BOTTOM
        };
        PIPS[10] = new float[] {
            LEFT,
            TOP,
            RIGHT,
            TOP,
            CENTER,
            (TOP + UPPER) / 2,
            LEFT,
            UPPER,
            RIGHT,
            UPPER,
            LEFT,
            LOWER,
            RIGHT,
            LOWER,
            CENTER,
            (LOWER + BOTTOM) / 2,
            LEFT,
            BOTTOM,
            RIGHT,
            BOTTOM
        };
    }

    private final Art art;
    private final Fonts fonts;
    private final I18NBundle text;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 turned = new Matrix4();
    private final Color ink = new Color();
    private final Color tinted = new Color();
    private final Color backdrop = new Color();
    private final Color nameInk = new Color();
    private Theme theme;
    private String language;

    CardPainter(Art art, Fonts fonts, I18NBundle text) {
        this.art = art;
        this.fonts = fonts;
        this.text = text;
    }

    void use(Theme theme, String language) {
        this.theme = theme;
        this.language = language;
    }

    String rank(Card card) {
        return switch (card.value()) {
            case Card.JACK -> text.get("rank.jack");
            case Card.QUEEN -> text.get("rank.queen");
            case Card.KING -> text.get("rank.king");
            case Card.ACE -> text.get("rank.ace");
            default -> Integer.toString(card.value());
        };
    }

    void face(Batch batch, Card card, float x, float y, float width, float alpha) {
        face(batch, card, x, y, width, alpha, theme);
    }

    void face(Batch batch, Card card, float x, float y, float width, float alpha, Theme theme) {
        float height = width * RATIO;
        shadow(batch, x, y, width, height, alpha);
        fill(batch, art.rounded, x, y, width, height, Palette.EDGE, alpha);
        fill(batch, art.rounded, x + 2, y + 2, width - 4, height - 4, Palette.FACE, alpha);
        ink.set(card.suit().type() == CardType.MONSTER ? Palette.INK : Palette.RED);
        ink.a = alpha;

        index(batch, card, x, y, width, height);
        // The second index is the first one turned upside down, like on a real card.
        float cx = x + width / 2;
        float cy = y + height / 2;
        saved.set(batch.getTransformMatrix());
        turned.set(saved).translate(cx, cy, 0).rotate(0, 0, 1, 180).translate(-cx, -cy, 0);
        batch.setTransformMatrix(turned);
        index(batch, card, x, y, width, height);
        batch.setTransformMatrix(saved);

        if (!theme.classic()) {
            themed(batch, card, theme, x, y, width, height);
        } else if (card.value() == Card.ACE) {
            pip(batch, card.suit(), cx, cy, width * 0.46f, false);
        } else if (card.value() > 10) {
            court(batch, card, x, y, width, height);
        } else {
            float[] pips = PIPS[card.value()];
            for (int i = 0; i < pips.length; i += 2) {
                float py = pips[i + 1];
                pip(batch, card.suit(), x + pips[i] * width, y + py * height, width * 0.16f, py < MIDDLE);
            }
        }
        batch.setColor(Color.WHITE);
    }

    void back(Batch batch, float x, float y, float width, float alpha) {
        float height = width * RATIO;
        float margin = width * 0.045f;
        float line = width * 0.09f;
        shadow(batch, x, y, width, height, alpha);
        fill(batch, art.rounded, x, y, width, height, Palette.FACE, alpha);
        fill(batch, art.rounded, x + margin, y + margin, width - 2 * margin, height - 2 * margin, Palette.BACK, alpha);
        fill(batch, art.rounded, x + line, y + line, width - 2 * line, height - 2 * line, Palette.BACK_LINE, alpha);
        line += 2.5f;
        fill(batch, art.rounded, x + line, y + line, width - 2 * line, height - 2 * line, Palette.BACK, alpha);
        ink.set(Palette.BACK_LINE);
        ink.a = alpha;
        pip(batch, Suit.DIAMONDS, x + width / 2, y + height / 2, width * 0.34f, false);
        batch.setColor(Color.WHITE);
    }

    void slot(Batch batch, float x, float y, float width, float alpha) {
        fill(batch, art.rounded, x, y, width, width * RATIO, Color.WHITE, alpha * 0.07f);
        batch.setColor(Color.WHITE);
    }

    /** The rank and suit in a corner, both centered on the same column, so a 10 lines up with every other card. */
    private void index(Batch batch, Card card, float x, float y, float width, float height) {
        float middle = x + width * 0.133f;
        float top = y + height - width * 0.06f;
        BitmapFont font = fonts.index;
        float previous = scale(font, width / DESIGN_WIDTH);
        String rank = rank(card);
        if (rank.length() > 1) {
            // Set narrower, as on printed cards, so a 10 centered over its suit stays clear of the card's edge.
            font.getData().setScale(font.getScaleX() * 0.85f, font.getScaleY());
        }
        drawRank(batch, font, rank, middle - rankWidth(font, rank) / 2, top);
        float capHeight = font.getData().capHeight;
        font.getData().setScale(previous);

        float size = width * 0.13f;
        pip(batch, card.suit(), middle, top - capHeight - width * 0.035f - size / 2, size, false);
    }

    /** The rank's width with each character set right after the previous one's ink, as {@link #drawRank} sets it. */
    private float rankWidth(BitmapFont font, String rank) {
        float width = 0;
        for (int i = 0; i < rank.length(); i++) {
            layout.setText(font, rank, i, i + 1, ink, 0, Align.left, false, null);
            width += layout.width;
        }
        return width;
    }

    /**
     * Draws the rank one character at a time, each placed right after the previous one's ink, so a 10 sits as close
     * together as on a printed card.
     */
    private void drawRank(Batch batch, BitmapFont font, String rank, float left, float top) {
        float x = left;
        for (int i = 0; i < rank.length(); i++) {
            layout.setText(font, rank, i, i + 1, ink, 0, Align.left, false, null);
            font.draw(batch, layout, x, top);
            x += layout.width;
        }
    }

    private void court(Batch batch, Card card, float x, float y, float width, float height) {
        frame(batch, x, y, width, height, ink, Palette.FACE);
        String rank = rank(card);
        BitmapFont font = fonts.face;
        float previous = scale(font, width / DESIGN_WIDTH);
        layout.setText(font, rank);
        font.setColor(ink);
        font.draw(batch, rank, x + (width - layout.width) / 2, y + height * 0.6f + font.getData().capHeight / 2);
        font.getData().setScale(previous);
        pip(batch, card.suit(), x + width / 2, y + height * 0.33f, width * 0.22f, false);
    }

    /** A theme's card: its icon and name, framed in the color of the card's family. The icon comes outlined in it. */
    private void themed(Batch batch, Card card, Theme theme, float x, float y, float width, float height) {
        Color tint = theme.tint(card.suit());
        tinted.set(tint);
        tinted.a = ink.a;
        backdrop.set(Palette.FACE).lerp(tint, 0.09f);
        frame(batch, x, y, width, height, tinted, backdrop);

        float size = width * 0.4f;
        float iconY = y + height * 0.59f - size / 2;
        float margin = size * Theme.OUTLINE_MARGIN;
        batch.setColor(1, 1, 1, ink.a);
        batch.draw(
                theme.icon(card),
                x + (width - size) / 2 - margin,
                iconY - margin,
                size + 2 * margin,
                size + 2 * margin);

        BitmapFont font = fonts.name;
        String name = theme.name(card, language);
        float available = width * (FRAME_WIDTH - 0.04f);
        nameInk.set(Palette.INK);
        nameInk.a = ink.a;
        float previous = scale(font, width / DESIGN_WIDTH);
        // Wrapping would split a word wider than the line, so the name shrinks until its longest word fits.
        float longest = longestWord(font, name);
        if (longest > available) {
            scale(font, available / longest);
        }
        layout.setText(font, name, nameInk, available, Align.center, true);
        font.draw(batch, layout, x + (width - available) / 2, iconY - width * 0.05f);
        font.getData().setScale(previous);
    }

    /** A hairline around the middle of the card, like the border around a court card's portrait. */
    private void frame(Batch batch, float x, float y, float width, float height, Color line, Color inside) {
        float frameX = x + width * FRAME_LEFT;
        float frameY = y + height * FRAME_BOTTOM;
        float frameWidth = width * FRAME_WIDTH;
        float frameHeight = height * FRAME_HEIGHT;
        float thickness = Math.max(1.5f, width * 0.012f);
        fill(batch, art.rounded, frameX, frameY, frameWidth, frameHeight, line, 1);
        fill(
                batch,
                art.rounded,
                frameX + thickness,
                frameY + thickness,
                frameWidth - 2 * thickness,
                frameHeight - 2 * thickness,
                inside,
                line.a);
    }

    private float longestWord(BitmapFont font, String text) {
        float longest = 0;
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf(' ', start);
            if (end < 0) {
                end = text.length();
            }
            layout.setText(font, text, start, end, nameInk, 0, Align.left, false, null);
            longest = Math.max(longest, layout.width);
            start = end + 1;
        }
        return longest;
    }

    private void pip(Batch batch, Suit suit, float cx, float cy, float size, boolean upsideDown) {
        TextureRegion region = art.suit(suit);
        batch.setColor(ink);
        float half = size / 2;
        batch.draw(region, cx - half, cy - half, half, half, size, size, 1, 1, upsideDown ? 180 : 0);
    }

    private void shadow(Batch batch, float x, float y, float width, float height, float alpha) {
        float spread = art.shadowSpread();
        batch.setColor(0, 0, 0, 0.35f * alpha);
        art.shadow.draw(batch, x - spread, y - spread - 3, width + 2 * spread, height + 2 * spread);
    }

    /** Scales a font for one card and returns its previous scale, to set back once the card is drawn. */
    private static float scale(BitmapFont font, float factor) {
        float previous = font.getScaleX();
        if (factor != 1) {
            font.getData().setScale(previous * factor);
        }
        return previous;
    }

    private static void fill(
            Batch batch, NinePatch patch, float x, float y, float width, float height, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, color.a * alpha);
        patch.draw(batch, x, y, width, height);
    }
}
