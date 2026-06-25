package li.cil.oc.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LanguageResourceTest {
    private static final Path EN_US = Path.of("src/main/resources/assets/neoopencomputers/lang/en_us.json");

    @Test
    void visibleManualBrandingUsesNeoOpenComputersName() throws IOException {
        JsonObject translations = readJson(EN_US);
        List<String> keys = List.of(
            "item.neoopencomputers.manual",
            "gui.neoopencomputers.manual",
            "oc:gui.Manual.Blocks",
            "oc:gui.Manual.Items");

        for (String key : keys) {
            assertTrue(translations.has(key), "Missing translation key " + key);
            String value = translations.get(key).getAsString();
            assertTrue(value.contains("NeoOpenComputers"), "Visible manual branding must use NeoOpenComputers in " + key + ": " + value);
        }
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
