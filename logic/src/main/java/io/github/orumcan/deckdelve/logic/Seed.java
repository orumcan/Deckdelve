package io.github.orumcan.deckdelve.logic;

/**
 * The seed that shuffles a run's dungeon: 40 bits, written as eight Crockford Base32 characters such as
 * {@code 7KQ2-M9XD}. The same seed and rules deal the same dungeon on every platform.
 */
public record Seed(long value) {
    public static final int BITS = 40;

    private static final long MASK = (1L << BITS) - 1;
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = BITS / 5;

    public Seed {
        if ((value & ~MASK) != 0) {
            throw new IllegalArgumentException("Seeds have " + BITS + " bits: " + value);
        }
    }

    public static Seed of(long bits) {
        return new Seed(bits & MASK);
    }

    /**
     * Reads a seed written by {@link #toString()}. Case and hyphens don't matter, and the easily confused letters I, L
     * and O read as 1, 1 and 0.
     */
    public static Seed parse(String text) {
        long value = 0;
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            if (c == '-') {
                continue;
            }
            int digit = ALPHABET.indexOf(c == 'I' || c == 'L' ? '1' : c == 'O' ? '0' : c);
            if (digit < 0 || length == LENGTH) {
                throw new IllegalArgumentException("Not a seed: " + text);
            }
            value = value << 5 | digit;
            length++;
        }
        if (length != LENGTH) {
            throw new IllegalArgumentException("Not a seed: " + text);
        }
        return new Seed(value);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(LENGTH + 1);
        for (int i = LENGTH - 1; i >= 0; i--) {
            text.append(ALPHABET.charAt((int) (value >>> 5 * i) & 31));
            if (i == LENGTH / 2) {
                text.append('-');
            }
        }
        return text.toString();
    }
}
