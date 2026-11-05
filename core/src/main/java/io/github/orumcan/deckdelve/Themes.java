package io.github.orumcan.deckdelve;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The themes the game ships: Classic Cards, plus every folder listed in themes/themes.json. The list is a file because
 * asset folders can't be listed inside a packaged jar or on the web.
 */
final class Themes {
    static final String DEFAULT = "fantasy";

    private final Map<String, ThemeData> data = new LinkedHashMap<>();

    Themes() {
        for (JsonValue id : new JsonReader().parse(Gdx.files.internal("themes/themes.json"))) {
            String folder = id.asString();
            JsonValue json = new JsonReader().parse(Gdx.files.internal("themes/" + folder + "/theme.json"));
            data.put(folder, ThemeData.parse(json));
        }
    }

    /** Classic Cards first, then the data themes in the order themes.json lists them. */
    List<String> ids() {
        List<String> ids = new ArrayList<>();
        ids.add(Theme.CLASSIC);
        ids.addAll(data.keySet());
        return Collections.unmodifiableList(ids);
    }

    boolean exists(String id) {
        return id.equals(Theme.CLASSIC) || data.containsKey(id);
    }

    /** The parsed theme.json, or null for Classic Cards, which has none. */
    ThemeData data(String id) {
        return data.get(id);
    }

    Theme load(String id) {
        return id.equals(Theme.CLASSIC) ? Theme.classicCards() : Theme.load(id, data.get(id));
    }
}
