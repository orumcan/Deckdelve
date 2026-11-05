package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ThemeDataTest {
    private static final Path THEMES = Path.of("../assets/themes");
    private static final int PNG_SIGNATURE = 0x89504E47;

    @Test
    void theDefaultThemeIsListed() throws IOException {
        assertTrue(listed().contains(Themes.DEFAULT));
    }

    @Test
    void everyListedThemeHasAllFortyFourCardsWithSquareIcons() throws IOException {
        for (String id : listed()) {
            ThemeData theme = ThemeData.parse(json(id));
            assertEquals(44, theme.cards().size(), id);
            for (ThemeData.Entry card : theme.cards().values()) {
                assertSquarePng(THEMES.resolve(id).resolve("icons").resolve(card.icon()));
            }
        }
    }

    @Test
    void everyIconFileBelongsToACard() throws IOException {
        for (String id : listed()) {
            Set<String> used = new HashSet<>();
            for (ThemeData.Entry card : ThemeData.parse(json(id)).cards().values()) {
                used.add(card.icon());
            }
            try (Stream<Path> files = Files.list(THEMES.resolve(id).resolve("icons"))) {
                files.forEach(file ->
                        assertTrue(used.contains(file.getFileName().toString()), file + " isn't used by any card"));
            }
        }
    }

    @Test
    void aThemeMissingACardIsRejected() throws IOException {
        JsonValue json = json(Themes.DEFAULT);
        json.get("cards").remove("AS");
        assertThrows(IllegalArgumentException.class, () -> ThemeData.parse(json));
    }

    @Test
    void textFallsBackToEnglish() {
        Localized name = Localized.of(new JsonReader().parse("{\"en\": \"Dragon\", \"de\": \"Drache\"}"));
        assertEquals("Drache", name.in("de"));
        assertEquals("Dragon", name.in("fr"));
    }

    @Test
    void textWithoutEnglishIsRejected() {
        JsonValue german = new JsonReader().parse("{\"de\": \"Drache\"}");
        assertThrows(IllegalArgumentException.class, () -> Localized.of(german));
    }

    private static List<String> listed() throws IOException {
        List<String> ids = new ArrayList<>();
        for (JsonValue id : new JsonReader().parse(Files.readString(THEMES.resolve("themes.json")))) {
            ids.add(id.asString());
        }
        return ids;
    }

    private static JsonValue json(String id) throws IOException {
        return new JsonReader().parse(Files.readString(THEMES.resolve(id).resolve("theme.json")));
    }

    /** A PNG's header holds its width and height right after the signature and the IHDR chunk's name. */
    private static void assertSquarePng(Path icon) throws IOException {
        assertTrue(Files.isRegularFile(icon), icon + " is missing");
        ByteBuffer header = ByteBuffer.wrap(Files.readAllBytes(icon));
        assertEquals(PNG_SIGNATURE, header.getInt(0), icon + " isn't a PNG");
        assertEquals(header.getInt(16), header.getInt(20), icon + " isn't square");
    }
}
