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

final class AssetModelResourceTest {
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/neoopencomputers");
    private static final Path MODEL_ROOT = ASSET_ROOT.resolve("models");
    private static final Path TEXTURE_ROOT = ASSET_ROOT.resolve("textures");
    private static final Set<String> CLIENT_LOG_MISSING_TEXTURE_REFS = Set.of(
        "minecraft:item/chest",
        "minecraft:item/clock",
        "minecraft:item/compass",
        "minecraft:item/daylight_detector",
        "minecraft:item/debug_stick",
        "minecraft:item/redstone_torch",
        "minecraft:item/stone_button",
        "minecraft:item/tripwire_hook"
    );

    @Test
    void manualItemModelExistsForClientResourceReload() {
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/manual.json")), "Missing manual item model");
    }

    @Test
    void robotFoundationModelsExistForClientResourceReload() {
        assertTrue(Files.exists(ASSET_ROOT.resolve("blockstates/robot.json")), "Missing robot blockstate");
        assertTrue(Files.exists(MODEL_ROOT.resolve("block/robot.json")), "Missing robot block model");
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/robot.json")), "Missing robot item model");
    }

    @Test
    void neoOpenComputersTextureReferencesHaveLocalPngs() throws IOException {
        List<String> missing = new ArrayList<>();
        for (TextureReference reference : textureReferences()) {
            if (reference.texture().startsWith("neoopencomputers:")) {
                Path texture = TEXTURE_ROOT.resolve(reference.texture().substring("neoopencomputers:".length()) + ".png");
                if (!Files.exists(texture)) {
                    missing.add(reference.model() + " -> " + reference.texture());
                }
            }
        }

        assertTrue(missing.isEmpty(), "Missing model textures: " + missing);
    }

    @Test
    void itemModelsAvoidTextureReferencesMissingInClientSmoke() throws IOException {
        List<String> invalid = new ArrayList<>();
        for (TextureReference reference : textureReferences()) {
            if (CLIENT_LOG_MISSING_TEXTURE_REFS.contains(reference.texture())) {
                invalid.add(reference.model() + " -> " + reference.texture());
            }
        }

        assertTrue(invalid.isEmpty(), "Client smoke reported missing texture refs: " + invalid);
    }

    private static List<TextureReference> textureReferences() throws IOException {
        List<TextureReference> references = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(MODEL_ROOT)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("textures")) {
                        continue;
                    }
                    JsonObject textures = json.getAsJsonObject("textures");
                    for (String key : textures.keySet()) {
                        references.add(new TextureReference(MODEL_ROOT.relativize(path).toString(), textures.get(key).getAsString()));
                    }
                }
            }
        }
        return references;
    }

    private record TextureReference(String model, String texture) {
    }
}
