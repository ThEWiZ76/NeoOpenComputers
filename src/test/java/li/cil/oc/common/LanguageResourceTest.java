package li.cil.oc.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LanguageResourceTest {
    private static final Path EN_US = Path.of("src/main/resources/assets/neoopencomputers/lang/en_us.json");
    private static final Path MOD_CONTENT_IDS = Path.of("src/main/java/li/cil/oc/common/ModContentIds.java");
    private static final Path MOD_BLOCKS = Path.of("src/main/java/li/cil/oc/common/ModBlocks.java");
    private static final Path MOD_ITEMS = Path.of("src/main/java/li/cil/oc/common/ModItems.java");
    private static final Pattern CONTENT_ID = Pattern.compile("public static final String\\s+(\\w+)\\s*=\\s*\"([^\"]+)\";");
    private static final Pattern REGISTERED_CONTENT_ID = Pattern.compile("ModContentIds\\.(\\w+)");

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

    @Test
    void registeredBlocksAndItemsHaveEnglishTranslations() throws IOException {
        JsonObject translations = readJson(EN_US);
        Map<String, String> contentIds = contentIds();

        for (String id : registeredIds(MOD_BLOCKS, contentIds)) {
            assertTrue(translations.has("block.neoopencomputers." + id), "Missing block translation for " + id);
        }
        for (String id : registeredIds(MOD_ITEMS, contentIds)) {
            String blockKey = "block.neoopencomputers." + id;
            String itemKey = "item.neoopencomputers." + id;
            assertTrue(translations.has(blockKey) || translations.has(itemKey), "Missing item/block translation for " + id);
        }
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Map<String, String> contentIds() throws IOException {
        String source = Files.readString(MOD_CONTENT_IDS);
        Matcher matcher = CONTENT_ID.matcher(source);
        Map<String, String> ids = new LinkedHashMap<>();
        while (matcher.find()) {
            ids.put(matcher.group(1), matcher.group(2));
        }
        return ids;
    }

    private static Collection<String> registeredIds(final Path sourcePath, final Map<String, String> contentIds) throws IOException {
        String source = Files.readString(sourcePath);
        Matcher matcher = REGISTERED_CONTENT_ID.matcher(source);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            String id = contentIds.get(matcher.group(1));
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
