package io.github.orumcan.deckdelve;

/** The outline drawn behind a theme's icon in the suit's tint: the icon's shape, widened by a few pixels. */
final class Outline {
    private Outline() {}

    /**
     * Takes a square icon's coverage, from 0 to 1, row by row. Each pixel of the outline takes the strongest coverage
     * within the radius, with the rim softened over a pixel so the edge stays smooth. The icon needs a transparent
     * margin as wide as the radius, or its outline is cut off at the edges.
     */
    static float[] of(float[] icon, int size, float radius) {
        int reach = (int) Math.ceil(radius + 0.5f);
        int[] dx = new int[(2 * reach + 1) * (2 * reach + 1)];
        int[] dy = new int[dx.length];
        float[] weight = new float[dx.length];
        int count = 0;
        // Nearest offsets first, so a pixel well inside the icon stops at the first one.
        for (int distance = 0; distance <= 2 * reach * reach; distance++) {
            float rim = radius + 0.5f - (float) Math.sqrt(distance);
            if (rim <= 0) {
                break;
            }
            for (int y = -reach; y <= reach; y++) {
                for (int x = -reach; x <= reach; x++) {
                    if (x * x + y * y == distance) {
                        dx[count] = x;
                        dy[count] = y;
                        weight[count] = Math.min(1, rim);
                        count++;
                    }
                }
            }
        }
        float[] outline = new float[icon.length];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float strongest = 0;
                for (int k = 0; k < count && strongest < 1; k++) {
                    int sx = x + dx[k];
                    int sy = y + dy[k];
                    if (sx >= 0 && sx < size && sy >= 0 && sy < size) {
                        strongest = Math.max(strongest, icon[sy * size + sx] * weight[k]);
                    }
                }
                outline[y * size + x] = strongest;
            }
        }
        return outline;
    }
}
