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
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class BlockModelResourceTest {
    private static final Path BLOCK_MODEL_ROOT = Path.of("src/main/resources/assets/neoopencomputers/models/block");
    private static final Path BLOCK_TEXTURE_ROOT = Path.of("src/main/resources/assets/neoopencomputers/textures/block");
    private static final Set<String> INTENTIONAL_VANILLA_TEXTURES = Set.of(
        "endstone.json all -> minecraft:block/end_stone",
        "print.json particle -> minecraft:block/stone"
    );

    @Test
    void blockModelsUseExistingModTexturesInsteadOfVanillaPlaceholders() throws IOException {
        final List<String> placeholders = new ArrayList<>();
        final List<String> missingTextures = new ArrayList<>();
        final List<String> unexpectedNamespaces = new ArrayList<>();
        try (Stream<Path> models = Files.list(BLOCK_MODEL_ROOT)) {
            for (final Path model : models.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(model)) {
                    final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!root.has("textures")) {
                        continue;
                    }
                    final JsonObject textures = root.getAsJsonObject("textures");
                    for (final String key : textures.keySet()) {
                        final String texture = textures.get(key).getAsString();
                        final String reference = model.getFileName() + " " + key + " -> " + texture;
                        if (texture.startsWith("minecraft:block/")) {
                            if (!INTENTIONAL_VANILLA_TEXTURES.contains(reference)) {
                                placeholders.add(reference);
                            }
                        } else if (texture.startsWith("neoopencomputers:block/")) {
                            final String path = texture.substring("neoopencomputers:block/".length());
                            if (!Files.exists(BLOCK_TEXTURE_ROOT.resolve(path + ".png"))) {
                                missingTextures.add(reference);
                            }
                        } else if (texture.contains(":") && !texture.startsWith("neoopencomputers:item/")) {
                            unexpectedNamespaces.add(reference);
                        }
                    }
                }
            }
        }

        assertTrue(placeholders.isEmpty(), "Block models still use vanilla placeholder textures: " + placeholders);
        assertTrue(missingTextures.isEmpty(), "Block models reference missing mod textures: " + missingTextures);
        assertTrue(unexpectedNamespaces.isEmpty(), "Block models reference unexpected texture namespaces: " + unexpectedNamespaces);
    }

    @Test
    void screenBlockModelsUseScreenPanelTexturesInsteadOfGenericCube() throws IOException {
        for (final String modelName : List.of("screen_tier1.json", "screen_tier2.json", "screen_tier3.json")) {
            try (Reader reader = Files.newBufferedReader(BLOCK_MODEL_ROOT.resolve(modelName))) {
                final JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
                assertFalse("minecraft:block/cube_bottom_top".equals(model.get("parent").getAsString()), modelName);
                final JsonObject textures = model.getAsJsonObject("textures");
                assertTrue(textures.get("front").getAsString().startsWith("neoopencomputers:block/screen/"), modelName);
                assertTrue(textures.get("back").getAsString().startsWith("neoopencomputers:block/screen/"), modelName);
            }
        }
    }
}
