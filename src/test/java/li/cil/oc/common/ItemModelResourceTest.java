package li.cil.oc.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ItemModelResourceTest {
    private static final Path ITEM_MODEL_ROOT = Path.of("src/main/resources/assets/neoopencomputers/models/item");
    private static final Path ITEM_TEXTURE_ROOT = Path.of("src/main/resources/assets/neoopencomputers/textures/item");

    @Test
    void itemModelsUseExistingModTexturesInsteadOfVanillaPlaceholders() throws IOException {
        final List<String> placeholders = new ArrayList<>();
        final List<String> missingTextures = new ArrayList<>();
        try (Stream<Path> models = Files.list(ITEM_MODEL_ROOT)) {
            for (final Path model : models.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(model)) {
                    final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!root.has("textures")) {
                        continue;
                    }
                    final JsonObject textures = root.getAsJsonObject("textures");
                    if (textures.has("layer0") && textures.get("layer0").getAsString().startsWith("minecraft:item/")) {
                        placeholders.add(model.getFileName() + " -> " + textures.get("layer0").getAsString());
                    }
                    if (textures.has("layer0") && textures.get("layer0").getAsString().startsWith("neoopencomputers:item/")) {
                        final String texture = textures.get("layer0").getAsString().substring("neoopencomputers:item/".length());
                        if (!Files.exists(ITEM_TEXTURE_ROOT.resolve(texture + ".png"))) {
                            missingTextures.add(model.getFileName() + " -> " + textures.get("layer0").getAsString());
                        }
                    }
                }
            }
        }

        assertTrue(placeholders.isEmpty(), "Item models still use vanilla placeholder textures: " + placeholders);
        assertTrue(missingTextures.isEmpty(), "Item models reference missing mod textures: " + missingTextures);
    }
}
