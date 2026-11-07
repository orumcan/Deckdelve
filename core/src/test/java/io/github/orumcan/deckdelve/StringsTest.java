package io.github.orumcan.deckdelve;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.orumcan.deckdelve.logic.Achievement;
import io.github.orumcan.deckdelve.logic.Modifier;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class StringsTest {
    @Test
    void everyModifierAndAchievementHasANameAndADescription() throws IOException {
        Properties strings = new Properties();
        try (Reader reader =
                Files.newBufferedReader(Path.of("../assets/i18n/strings.properties"), StandardCharsets.UTF_8)) {
            strings.load(reader);
        }
        for (Modifier modifier : Modifier.values()) {
            assertNamed(strings, Ui.key("modifier", modifier));
        }
        for (Achievement achievement : Achievement.values()) {
            assertNamed(strings, Ui.key("achievement", achievement));
        }
    }

    private static void assertNamed(Properties strings, String key) {
        assertTrue(strings.containsKey(key), key + " is missing");
        assertTrue(strings.containsKey(key + ".body"), key + ".body is missing");
    }
}
