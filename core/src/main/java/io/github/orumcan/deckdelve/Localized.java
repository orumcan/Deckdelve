package io.github.orumcan.deckdelve;

import com.badlogic.gdx.utils.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Text a theme gives in several languages, such as {@code {"en": "Goblin"}}. English is required and the fallback. */
record Localized(Map<String, String> byLanguage) {
    static Localized of(JsonValue json) {
        Map<String, String> texts = new HashMap<>();
        for (JsonValue text : json) {
            texts.put(text.name, text.asString());
        }
        if (!texts.containsKey("en")) {
            throw new IllegalArgumentException("No English text in " + json);
        }
        return new Localized(Collections.unmodifiableMap(texts));
    }

    String in(String language) {
        String text = byLanguage.get(language);
        return text != null ? text : byLanguage.get("en");
    }
}
