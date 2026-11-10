package io.github.orumcan.deckdelve;

import io.github.orumcan.deckdelve.logic.Suit;

final class SuitShapes {
    private static final float SPREAD = 0.2f;
    /** How far each half of the broken heart is pulled from the crack. */
    private static final float BREAK = 0.035f;
    /** The crack's bends from the top of the heart to its tip, as pairs of height and place across. */
    private static final float[] CRACK = {1, 0.5f, 0.64f, 0.41f, 0.47f, 0.58f, 0.3f, 0.43f, 0, 0.5f};

    private SuitShapes() {}

    static Raster.Shape of(Suit suit) {
        return switch (suit) {
            case CLUBS -> club();
            case SPADES -> spade();
            case DIAMONDS -> (x, y) -> Math.abs(x - 0.5f) / 0.38f + Math.abs(y - 0.5f) / 0.48f <= 1;
            case HEARTS -> lobes(0.66f, 0.25f, 0.06f);
        };
    }

    /** The heart cracked in two down a jagged line, with its halves pulled apart. */
    static Raster.Shape brokenHeart() {
        Raster.Shape heart = of(Suit.HEARTS);
        return (x, y) -> {
            float crack = crack(y);
            return x + BREAK < crack && heart.contains(x + BREAK, y)
                    || x - BREAK >= crack && heart.contains(x - BREAK, y);
        };
    }

    private static float crack(float y) {
        for (int i = 2; i < CRACK.length; i += 2) {
            if (y >= CRACK[i]) {
                float t = (y - CRACK[i]) / (CRACK[i - 2] - CRACK[i]);
                return CRACK[i + 1] + t * (CRACK[i - 1] - CRACK[i + 1]);
            }
        }
        return 0.5f;
    }

    private static Raster.Shape spade() {
        Raster.Shape blade = lobes(0.42f, 0.22f, 0.97f);
        Raster.Shape stem = stem();
        return (x, y) -> blade.contains(x, y) || stem.contains(x, y);
    }

    private static Raster.Shape club() {
        float radius = 0.205f;
        Raster.Shape stem = stem();
        return (x, y) -> inCircle(x, y, 0.5f, 0.71f, radius)
                || inCircle(x, y, 0.5f - 0.215f, 0.47f, radius)
                || inCircle(x, y, 0.5f + 0.215f, 0.47f, radius)
                || inCircle(x, y, 0.5f, 0.52f, 0.12f)
                || stem.contains(x, y);
    }

    /** Two overlapping circles joined to a tip along their tangents: a heart, or a spade's blade when the tip is on top. */
    private static Raster.Shape lobes(float centerY, float radius, float tipY) {
        float[] left = tangent(0.5f - SPREAD, centerY, radius, tipY, -1);
        float[] right = tangent(0.5f + SPREAD, centerY, radius, tipY, 1);
        float[] polygon = {
            0.5f, tipY, right[0], right[1], 0.5f + SPREAD, centerY, 0.5f - SPREAD, centerY, left[0], left[1]
        };
        return (x, y) -> inCircle(x, y, 0.5f - SPREAD, centerY, radius)
                || inCircle(x, y, 0.5f + SPREAD, centerY, radius)
                || inConvex(x, y, polygon);
    }

    private static Raster.Shape stem() {
        float[] triangle = {0.5f, 0.5f, 0.35f, 0.02f, 0.65f, 0.02f};
        return (x, y) -> inConvex(x, y, triangle);
    }

    /** Where a line from the tip at (0.5, tipY) touches the circle, on its left (-1) or right (1) side. */
    private static float[] tangent(float cx, float cy, float radius, float tipY, int side) {
        float dx = 0.5f - cx;
        float dy = tipY - cy;
        float toTip = (float) Math.atan2(dy, dx);
        float offset = (float) Math.acos(radius / Math.sqrt(dx * dx + dy * dy));
        float a = toTip + offset;
        float b = toTip - offset;
        float angle = (Math.cos(a) - Math.cos(b)) * side > 0 ? a : b;
        return new float[] {cx + radius * (float) Math.cos(angle), cy + radius * (float) Math.sin(angle)};
    }

    private static boolean inCircle(float x, float y, float cx, float cy, float radius) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    /** Whether a point lies inside a convex polygon of x, y pairs, wound either way. */
    private static boolean inConvex(float x, float y, float[] points) {
        boolean left = false;
        boolean right = false;
        for (int i = 0; i < points.length; i += 2) {
            float ax = points[i];
            float ay = points[i + 1];
            float bx = points[(i + 2) % points.length];
            float by = points[(i + 3) % points.length];
            float cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax);
            left |= cross > 0;
            right |= cross < 0;
        }
        return !(left && right);
    }
}
