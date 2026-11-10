package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import io.github.orumcan.deckdelve.logic.Event;
import io.github.orumcan.deckdelve.logic.Status;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** The table's sound effects (D58), all at the volume picked in Settings. */
final class Sounds implements Disposable {
    enum Effect {
        DEAL("deal", 0.45f),
        TURN("turn", 0.5f),
        SHOVE("shove", 0.5f),
        WEAPON("weapon", 0.7f),
        SLASH("slash", 0.6f),
        PUNCH("punch", 0.6f),
        POTION("potion", 0.6f),
        BREAK("break", 0.7f),
        WIN("win", 0.6f),
        DEATH("death", 0.6f);

        private final String file;
        /** How loud the effect plays next to the others. The files all peak at the same level. */
        private final float level;

        Effect(String file, float level) {
            this.file = file;
            this.level = level;
        }

        /** The effect an event makes, if any: Hunger is silent. */
        static Optional<Effect> of(Event event) {
            if (event instanceof Event.CardDealt) {
                return Optional.of(DEAL);
            }
            if (event instanceof Event.CardRevealed) {
                return Optional.of(TURN);
            }
            if (event instanceof Event.RoomAvoided || event instanceof Event.PotionWasted) {
                return Optional.of(SHOVE);
            }
            if (event instanceof Event.WeaponEquipped) {
                return Optional.of(WEAPON);
            }
            if (event instanceof Event.MonsterFought fought) {
                return Optional.of(fought.usedWeapon() ? SLASH : PUNCH);
            }
            if (event instanceof Event.PotionDrunk) {
                return Optional.of(POTION);
            }
            if (event instanceof Event.WeaponBroke) {
                return Optional.of(BREAK);
            }
            if (event instanceof Event.GameOver over) {
                return Optional.of(over.status() == Status.WON ? WIN : DEATH);
            }
            return Optional.empty();
        }
    }

    private final Map<Effect, Sound> sounds = new EnumMap<>(Effect.class);
    private float volume;

    Sounds() {
        for (Effect effect : Effect.values()) {
            sounds.put(effect, Gdx.audio.newSound(Gdx.files.internal("sounds/" + effect.file + ".wav")));
        }
    }

    /** From 0, silent, to 1. */
    void volume(float volume) {
        this.volume = volume;
    }

    void play(Effect effect) {
        if (volume <= 0) {
            return;
        }
        // Cards dealt one after another vary a little in pitch, so a room doesn't sound like one card four times.
        float pitch = effect == Effect.DEAL ? MathUtils.random(0.94f, 1.06f) : 1;
        sounds.get(effect).play(volume * effect.level, pitch, 0);
    }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
    }
}
