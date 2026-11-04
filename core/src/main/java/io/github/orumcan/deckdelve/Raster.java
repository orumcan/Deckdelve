package io.github.orumcan.deckdelve;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;

/** Paints white, anti-aliased shapes into pixmaps, so every card and panel is drawn by code instead of image files. */
final class Raster {
    interface Coverage {
        float at(int x, int y);
    }

    interface Shape {
        boolean contains(float x, float y);
    }

    private static final int SAMPLES = 4;

    private Raster() {}

    static Pixmap paint(int width, int height, Coverage coverage) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.drawPixel(x, y, Color.rgba8888(1, 1, 1, coverage.at(x, y)));
            }
        }
        return pixmap;
    }

    /** Samples a shape given in unit coordinates, with y pointing up, several times per pixel. */
    static Pixmap paint(int size, Shape shape) {
        return paint(size, size, (x, y) -> {
            int hits = 0;
            for (int sy = 0; sy < SAMPLES; sy++) {
                for (int sx = 0; sx < SAMPLES; sx++) {
                    float u = (x + (sx + 0.5f) / SAMPLES) / size;
                    float v = 1 - (y + (sy + 0.5f) / SAMPLES) / size;
                    if (shape.contains(u, v)) {
                        hits++;
                    }
                }
            }
            return hits / (float) (SAMPLES * SAMPLES);
        });
    }

    /** A rounded square inset from the pixmap's edges. With a blur, the edge fades out over that many pixels. */
    static Pixmap roundedSquare(int size, float inset, float radius, float blur) {
        float center = size / 2f;
        float straight = center - inset - radius;
        return paint(size, size, (x, y) -> {
            float qx = Math.abs(x + 0.5f - center) - straight;
            float qy = Math.abs(y + 0.5f - center) - straight;
            float outside = (float) Math.sqrt(square(Math.max(qx, 0)) + square(Math.max(qy, 0)));
            float distance = outside + Math.min(Math.max(qx, qy), 0) - radius;
            if (blur <= 0) {
                return MathUtils.clamp(0.5f - distance, 0, 1);
            }
            float t = MathUtils.clamp((distance + blur) / (2 * blur), 0, 1);
            return 1 - t * t * (3 - 2 * t);
        });
    }

    private static float square(float value) {
        return value * value;
    }
}
