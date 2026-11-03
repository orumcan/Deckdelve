package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class DeckdelveGame extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}
