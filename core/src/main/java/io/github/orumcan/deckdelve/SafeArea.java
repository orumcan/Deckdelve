package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;

/**
 * The part of the screen that camera cutouts leave clear, measured for the layouts. The smallest world every layout is
 * designed for has to fit inside that part, while the world itself still covers the whole screen, so the table runs on
 * under a cutout and only what a player reads or taps keeps clear of it.
 *
 * @param pixelsPerUnit the screen pixels one world unit takes
 * @param left the room to leave at this edge, like the other three, in world units
 */
record SafeArea(float pixelsPerUnit, float left, float top, float right, float bottom) {
    static final SafeArea WHOLE_SCREEN = new SafeArea(1, 0, 0, 0, 0);

    /** The safe area as the platform reports it. Only Android knows of cutouts; desktop and the web report none. */
    static SafeArea of(int width, int height) {
        return of(
                width,
                height,
                Gdx.graphics.getSafeInsetLeft(),
                Gdx.graphics.getSafeInsetTop(),
                Gdx.graphics.getSafeInsetRight(),
                Gdx.graphics.getSafeInsetBottom());
    }

    /** Takes the screen's size and the insets that keep clear of its cutouts, all in the same pixels. */
    static SafeArea of(int width, int height, int left, int top, int right, int bottom) {
        float pixelsPerUnit = Math.min((width - left - right) / Ui.WIDTH, (height - top - bottom) / Ui.HEIGHT);
        return new SafeArea(
                pixelsPerUnit,
                left / pixelsPerUnit,
                top / pixelsPerUnit,
                right / pixelsPerUnit,
                bottom / pixelsPerUnit);
    }

    /** The smallest world for the whole screen: the layouts' own, plus the room the insets take. */
    float minWorldWidth() {
        return Ui.WIDTH + left + right;
    }

    float minWorldHeight() {
        return Ui.HEIGHT + top + bottom;
    }
}
