package io.github.orumcan.deckdelve.logic;

/**
 * SplitMix64 (Steele, Lea and Flood, 2014): a small, fast generator that produces the same numbers on every platform.
 * Seeds depend on it, so changing it changes every dungeon.
 */
final class SplitMix64 {
    private long state;

    SplitMix64(long seed) {
        state = seed;
    }

    long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("The bound must be positive: " + bound);
        }
        // Skipping the lowest (2^31 mod bound) values leaves a range that divides evenly by bound.
        int threshold = (Integer.MAX_VALUE - bound + 1) % bound;
        while (true) {
            int candidate = (int) (nextLong() >>> 33);
            if (candidate >= threshold) {
                return candidate % bound;
            }
        }
    }
}
