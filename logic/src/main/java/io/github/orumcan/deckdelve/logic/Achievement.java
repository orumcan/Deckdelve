package io.github.orumcan.deckdelve.logic;

public enum Achievement {
    /** Three weapons equipped back to back, each one higher than the last. */
    FULL_ARSENAL,
    /** Five kills in a row on one weapon, each exactly one lower than the last. */
    STRAIGHT,
    /** Five monsters of one suit on one weapon. */
    FLUSH,
    /** The ace, king, queen, jack and 10 of one suit, five kills in a row on one weapon. */
    ROYAL_FLUSH,
    /** Both monsters of one rank slain back to back with one weapon. */
    TWINS,
    /** Three monsters from one Room slain without losing health. */
    FLAWLESS,
    /** An ace slain with the 2 of diamonds, surviving the blow. */
    UNDERDOG,
    /** Both aces slain with a weapon in one run. */
    DRAGON_SLAYER,
    /** A win with at least seven barehanded kills. */
    BARE_KNUCKLES,
    /** An ace slain barehanded, in a run that's won. */
    KNOCKOUT,
    /** A win without avoiding a Room. */
    IRONMAN,
    /** A win after avoiding at least five Rooms. */
    ESCAPE_ARTIST,
    /** A win without a second potion in any Room. */
    WASTE_NOT,
    /** A win with exactly 1 health. */
    LAST_GASP,
    /** A win with a plain score above 20, before any multiplier. */
    OVERFLOWING,
    /** A win with a plain score of 30: full health, ending on the 10 of hearts. */
    PERFECT,
    /** A win with three or more modifiers on. */
    MASOCHIST,
    /** Three wins in a row. */
    HAT_TRICK,
    /** A win with each modifier on, over any number of runs. */
    CHALLENGER,
    /** Ten wins. */
    VETERAN
}
