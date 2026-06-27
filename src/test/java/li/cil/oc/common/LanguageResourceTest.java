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

    @Test
    void rackAndServerGuiTranslationsExistForFirstSmokeTesting() throws IOException {
        JsonObject translations = readJson(EN_US);
        List<String> keys = List.of(
            "oc:container.server",
            "gui.neoopencomputers.server_rack",
            "gui.neoopencomputers.server_rack.slot.card",
            "gui.neoopencomputers.server_rack.slot.component_bus",
            "gui.neoopencomputers.server_rack.slot.cpu",
            "gui.neoopencomputers.server_rack.slot.eeprom",
            "gui.neoopencomputers.server_rack.slot.empty",
            "gui.neoopencomputers.server_rack.slot.empty_state",
            "gui.neoopencomputers.server_rack.slot.hdd",
            "gui.neoopencomputers.server_rack.slot.installed",
            "gui.neoopencomputers.server_rack.slot.any_tier",
            "gui.neoopencomputers.server_rack.slot.max_tier",
            "gui.neoopencomputers.server_rack.slot.memory",
            "gui.neoopencomputers.server_rack.slot.unavailable",
            "gui.neoopencomputers.server_rack.components",
            "gui.neoopencomputers.server_rack.power.turn_off",
            "gui.neoopencomputers.server_rack.power.turn_on",
            "gui.neoopencomputers.server_rack.status",
            "gui.neoopencomputers.server_rack.state.empty",
            "gui.neoopencomputers.server_rack.state.incomplete",
            "gui.neoopencomputers.server_rack.state.ready",
            "gui.neoopencomputers.server_rack.state.running",
            "gui.neoopencomputers.computer_case.power.turn_off",
            "gui.neoopencomputers.computer_case.power.turn_on",
            "gui.neoopencomputers.rack.bus",
            "gui.neoopencomputers.rack.bus.clear",
            "gui.neoopencomputers.rack.bus.map",
            "gui.neoopencomputers.rack.bus.label.back",
            "gui.neoopencomputers.rack.bus.label.bottom",
            "gui.neoopencomputers.rack.bus.label.front",
            "gui.neoopencomputers.rack.bus.label.left",
            "gui.neoopencomputers.rack.bus.label.right",
            "gui.neoopencomputers.rack.bus.label.top",
            "gui.neoopencomputers.rack.bus.label.unknown",
            "gui.neoopencomputers.rack.bus.side.back",
            "gui.neoopencomputers.rack.bus.side.bottom",
            "gui.neoopencomputers.rack.bus.side.front",
            "gui.neoopencomputers.rack.bus.side.left",
            "gui.neoopencomputers.rack.bus.side.right",
            "gui.neoopencomputers.rack.bus.side.top",
            "gui.neoopencomputers.rack.bus.side.unknown",
            "gui.neoopencomputers.rack.control",
            "gui.neoopencomputers.rack.missing.cpu",
            "gui.neoopencomputers.rack.missing.eeprom",
            "gui.neoopencomputers.rack.missing.memory",
            "gui.neoopencomputers.rack.orientation.line1",
            "gui.neoopencomputers.rack.orientation.line2",
            "gui.neoopencomputers.rack.orientation.line3",
            "gui.neoopencomputers.rack.orientation.line4",
            "gui.neoopencomputers.rack.relay",
            "gui.neoopencomputers.rack.relay.disabled",
            "gui.neoopencomputers.rack.relay.enabled",
            "gui.neoopencomputers.rack.state.empty",
            "gui.neoopencomputers.rack.state.incomplete",
            "gui.neoopencomputers.rack.state.ready",
            "gui.neoopencomputers.rack.state.running");

        for (String key : keys) {
            assertTrue(translations.has(key), "Missing rack/server GUI translation key " + key);
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
