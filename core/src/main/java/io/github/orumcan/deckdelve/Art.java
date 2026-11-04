package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import io.github.orumcan.deckdelve.logic.Suit;

final class Art implements Disposable {
    /** World units per texel: drawing the patches at half size keeps their corners sharp on dense screens. */
    private static final float PATCH_SCALE = 0.5f;

    final NinePatch rounded;
    final NinePatch shadow;
    final NinePatch wideShadow;
    final TextureRegion pixel;
    final TextureRegion vignette;
    private final TextureRegion[] suits = new TextureRegion[Suit.values().length];
    private final Array<Pixmap> pixmaps = new Array<>();
    private final Array<Texture> textures = new Array<>();

    Art() {
        rounded = new NinePatch(texture(Raster.roundedSquare(64, 0, 24, 0)), 24, 24, 24, 24);
        rounded.scale(PATCH_SCALE, PATCH_SCALE);
        Texture soft = texture(Raster.roundedSquare(96, 16, 24, 16));
        shadow = new NinePatch(soft, 40, 40, 40, 40);
        shadow.scale(PATCH_SCALE, PATCH_SCALE);
        wideShadow = new NinePatch(soft, 40, 40, 40, 40);
        pixel = new TextureRegion(texture(Raster.paint(4, 4, (x, y) -> 1)));
        vignette = new TextureRegion(texture(Raster.paint(64, 64, Art::vignette)));
        for (Suit suit : Suit.values()) {
            suits[suit.ordinal()] = new TextureRegion(texture(Raster.paint(128, SuitShapes.of(suit))));
        }
    }

    TextureRegion suit(Suit suit) {
        return suits[suit.ordinal()];
    }

    /** How far the shadow patch reaches past the shape it sits under. */
    float shadowSpread() {
        return 16 * PATCH_SCALE;
    }

    float wideShadowSpread() {
        return 16;
    }

    /** Darkens toward the edges and corners, like a lamp over the middle of the table. */
    private static float vignette(int x, int y) {
        float dx = (x + 0.5f) / 32 - 1;
        float dy = (y + 0.5f) / 32 - 1;
        float distance = (float) Math.sqrt((dx * dx + dy * dy) / 2);
        float t = MathUtils.clamp((distance - 0.35f) / 0.65f, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Managed, so Android can restore it after the app loses its GL context. */
    private Texture texture(Pixmap pixmap) {
        pixmaps.add(pixmap);
        Texture texture = new Texture(new PixmapTextureData(pixmap, null, true, false, true));
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        textures.add(texture);
        return texture;
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        for (Pixmap pixmap : pixmaps) {
            pixmap.dispose();
        }
    }
}
