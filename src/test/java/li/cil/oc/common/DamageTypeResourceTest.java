package li.cil.oc.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DamageTypeResourceTest {
    private static final String HUNGRY_DAMAGE_TYPE = "neoopencomputers:nanomachines_hungry";
    private static final Path DAMAGE_TYPE = Path.of("src/main/resources/data/neoopencomputers/damage_type/nanomachines_hungry.json");
    private static final Path BYPASSES_ARMOR = Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_armor.json");
    private static final Path BYPASSES_EFFECTS = Path.of("src/main/resources/data/minecraft/tags/damage_type/bypasses_effects.json");
    private static final Path EN_US = Path.of("src/main/resources/assets/neoopencomputers/lang/en_us.json");

    @Test
    void nanomachinesHungryDamageTypeUsesUpstreamMessageId() throws IOException {
        final JsonObject json = readJson(DAMAGE_TYPE);

        assertEquals("oc.nanomachinesHungry", json.get("message_id").getAsString());
        assertEquals("never", json.get("scaling").getAsString());
        assertEquals(0.0F, json.get("exhaustion").getAsFloat());
    }

    @Test
    void nanomachinesHungryDamageBypassesArmorAndEffects() throws IOException {
        assertTagContains(BYPASSES_ARMOR, HUNGRY_DAMAGE_TYPE);
        assertTagContains(BYPASSES_EFFECTS, HUNGRY_DAMAGE_TYPE);
    }

    @Test
    void nanomachinesHungryDamageHasUpstreamDeathMessages() throws IOException {
        final JsonObject json = readJson(EN_US);

        assertEquals("%s was eaten by nanomachines.", json.get("death.attack.oc.nanomachinesHungry.1").getAsString());
        assertEquals("%s didn't keep their nanomachines fed.", json.get("death.attack.oc.nanomachinesHungry.2").getAsString());
        assertEquals("%s has been digested.", json.get("death.attack.oc.nanomachinesHungry.3").getAsString());
    }

    private static void assertTagContains(final Path path, final String value) throws IOException {
        final JsonObject json = readJson(path);
        assertEquals(false, json.get("replace").getAsBoolean());
        final JsonArray values = json.getAsJsonArray("values");
        for (final JsonElement element : values) {
            if (value.equals(element.getAsString())) {
                return;
            }
        }
        assertTrue(false, path + " does not contain " + value);
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
