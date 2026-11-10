package io.github.orumcan.deckdelve;

/** How fast the table animates, picked in Settings (D58). */
enum AnimationSpeed {
    NORMAL(1),
    FAST(0.5f),
    OFF(0);

    /** Every animation's duration is multiplied by this. */
    final float scale;

    AnimationSpeed(float scale) {
        this.scale = scale;
    }

    boolean moves() {
        return this != OFF;
    }
}
