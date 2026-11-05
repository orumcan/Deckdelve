package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Disposable;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Suit;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/** How cards look: Classic Cards with pips, or a data theme with an icon and a name on every card. */
final class Theme implements Disposable {
    static final String CLASSIC = "classic";

    /** Icons are shrunk to this many pixels, so all 44, outlines included, fit one texture. */
    private static final int ICON_SIZE = 128;

    /** The outline's width in pixels: about the frame's line on a card at table size. */
    private static final float OUTLINE = 4;

    private static final int MARGIN = 5;
    private static final int ART_SIZE = ICON_SIZE + 2 * MARGIN;

    /** A pixel of space around each icon keeps its neighbors from bleeding into it when the texture is filtered. */
    private static final int CELL = ART_SIZE + 2;

    private static final int PAGE = 1024;
    private static final int COLUMNS = PAGE / CELL;

    /** How far an icon's outline reaches past its square, as a share of the square's size. */
    static final float OUTLINE_MARGIN = MARGIN / (float) ICON_SIZE;

    final String id;
    private final ThemeData data;
    private final Pixmap page;
    private final Texture texture;
    private final Map<Card, TextureRegion> icons = new HashMap<>();

    private Theme(String id, ThemeData data, Pixmap page, Texture texture) {
        this.id = id;
        this.data = data;
        this.page = page;
        this.texture = texture;
    }

    static Theme classicCards() {
        return new Theme(CLASSIC, null, null, null);
    }

    static Theme load(String id, ThemeData data) {
        byte[] pixels = new byte[PAGE * PAGE * 4];
        Map<Card, Integer> cells = new HashMap<>();
        for (Suit suit : Suit.values()) {
            for (int value = 2; value <= suit.highestValue(); value++) {
                Card card = new Card(suit, value);
                int cell = cells.size();
                FileHandle icon = Gdx.files.internal(
                        "themes/" + id + "/icons/" + data.cards().get(card).icon());
                paint(pixels, cell, icon, data.tints().get(suit));
                cells.put(card, cell);
            }
        }
        Pixmap page = new Pixmap(PAGE, PAGE, Pixmap.Format.RGBA8888);
        // Copied in at once: on the web, every drawPixel copies the whole pixmap.
        ByteBuffer buffer = page.getPixels();
        buffer.put(pixels);
        buffer.position(0);
        // Managed, so Android can restore it after the app loses its GL context.
        Texture texture = new Texture(new PixmapTextureData(page, null, true, false, true));
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        Theme theme = new Theme(id, data, page, texture);
        for (Map.Entry<Card, Integer> cell : cells.entrySet()) {
            theme.icons.put(
                    cell.getKey(),
                    new TextureRegion(texture, left(cell.getValue()), top(cell.getValue()), ART_SIZE, ART_SIZE));
        }
        return theme;
    }

    /**
     * Paints the icon into its cell: its white in the card's off-white and its black in the suit's tint, over an
     * outline in the tint. Grays mix the two.
     */
    private static void paint(byte[] pixels, int cell, FileHandle file, Color tint) {
        Pixmap icon = rgba(new Pixmap(file));
        int size = icon.getWidth();
        byte[] source = new byte[size * size * 4];
        icon.getPixels().get(source);
        icon.dispose();
        // Premultiplied by alpha, so shrinking averages the edges right, whatever color the transparent pixels hold.
        float[] coverage = new float[size * size];
        float[] light = new float[size * size];
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = (source[i * 4 + 3] & 0xff) / 255f;
            light[i] = coverage[i] * brightness(source, i * 4);
        }
        coverage = framed(shrink(coverage, size, ICON_SIZE));
        light = framed(shrink(light, size, ICON_SIZE));
        float[] outline = Outline.of(coverage, ART_SIZE, OUTLINE);
        Color face = Palette.FACE;
        for (int y = 0; y < ART_SIZE; y++) {
            for (int x = 0; x < ART_SIZE; x++) {
                int i = y * ART_SIZE + x;
                float shade = coverage[i] - light[i] + outline[i] * (1 - coverage[i]);
                float alpha = light[i] + shade;
                if (alpha > 0) {
                    int at = ((top(cell) + y) * PAGE + left(cell) + x) * 4;
                    pixels[at] = channel((face.r * light[i] + tint.r * shade) / alpha);
                    pixels[at + 1] = channel((face.g * light[i] + tint.g * shade) / alpha);
                    pixels[at + 2] = channel((face.b * light[i] + tint.b * shade) / alpha);
                    pixels[at + 3] = channel(alpha);
                }
            }
        }
    }

    /** Redraws the image as RGBA8888, so its pixels read the same whatever kind of PNG it came from. */
    private static Pixmap rgba(Pixmap image) {
        Pixmap rgba = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
        rgba.setBlending(Pixmap.Blending.None);
        rgba.drawPixmap(image, 0, 0);
        image.dispose();
        return rgba;
    }

    private static float brightness(byte[] rgba, int at) {
        return ((rgba[at] & 0xff) + (rgba[at + 1] & 0xff) + (rgba[at + 2] & 0xff)) / (3 * 255f);
    }

    /**
     * Shrinks a square image by averaging the pixels each new pixel covers, so no pixel is skipped and thin lines stay
     * smooth. A pixel split between two new ones counts in each by its share.
     */
    static float[] shrink(float[] image, int from, int to) {
        float[] narrowed = new float[from * to];
        for (int y = 0; y < from; y++) {
            for (int x = 0; x < to; x++) {
                narrowed[y * to + x] = average(image, y * from, 1, x, from, to);
            }
        }
        float[] shrunk = new float[to * to];
        for (int y = 0; y < to; y++) {
            for (int x = 0; x < to; x++) {
                shrunk[y * to + x] = average(narrowed, x, to, y, from, to);
            }
        }
        return shrunk;
    }

    /**
     * Averages the pixels a new pixel covers along one line of the image, which starts at first and steps by step.
     * Lengths are counted in 1/to of an old pixel, so every share is a whole number.
     */
    private static float average(float[] image, int first, int step, int index, int from, int to) {
        int start = index * from;
        int end = start + from;
        float sum = 0;
        for (int pixel = start / to; pixel * to < end; pixel++) {
            int share = Math.min((pixel + 1) * to, end) - Math.max(pixel * to, start);
            sum += image[first + pixel * step] * share;
        }
        return sum / from;
    }

    /** Puts the icon in the middle of its art, with room around it for the outline. */
    private static float[] framed(float[] icon) {
        float[] art = new float[ART_SIZE * ART_SIZE];
        for (int y = 0; y < ICON_SIZE; y++) {
            System.arraycopy(icon, y * ICON_SIZE, art, (y + MARGIN) * ART_SIZE + MARGIN, ICON_SIZE);
        }
        return art;
    }

    private static int left(int cell) {
        return cell % COLUMNS * CELL + 1;
    }

    private static int top(int cell) {
        return cell / COLUMNS * CELL + 1;
    }

    private static byte channel(float value) {
        return (byte) Math.round(value * 255);
    }

    boolean classic() {
        return data == null;
    }

    String name(Card card, String language) {
        return data.cards().get(card).name().in(language);
    }

    TextureRegion icon(Card card) {
        return icons.get(card);
    }

    Color tint(Suit suit) {
        return data.tints().get(suit);
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            page.dispose();
        }
    }
}
