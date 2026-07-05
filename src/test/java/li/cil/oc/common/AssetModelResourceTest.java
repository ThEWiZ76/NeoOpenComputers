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
    void robotAndDroneUseDedicatedVisualAssets() throws IOException {
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("model/robot.png")), "Missing upstream robot model texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("item/robot.png")), "Missing upstream robot item texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("model/drone.png")), "Missing upstream drone model texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("item/drone.png")), "Missing upstream drone item texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("item/drone_case_tier1.png")), "Missing tier 1 drone case item texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("item/drone_case_tier2.png")), "Missing tier 2 drone case item texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("item/drone_case_creative.png")), "Missing creative drone case item texture");
        assertTrue(Files.exists(TEXTURE_ROOT.resolve("block/robot.png")), "Missing robot block texture");

        final String robotModel = Files.readString(MODEL_ROOT.resolve("block/robot.json"));
        final String robotItemModel = Files.readString(MODEL_ROOT.resolve("item/robot.json"));
        final String droneModel = Files.readString(MODEL_ROOT.resolve("item/drone.json"));
        final String droneCaseModel = Files.readString(MODEL_ROOT.resolve("item/drone_case_tier1.json"));

        assertTrue(robotModel.contains("\"elements\": []"), "Robot block model should be invisible so the block entity renderer owns the world shape");
        assertTrue(robotModel.contains("neoopencomputers:block/robot"), "Robot block model should keep a robot particle texture");
        assertTrue(!robotModel.contains("minecraft:block/cube_all"), "Robot block model must not render as a cube");
        assertTrue(!robotModel.contains("computer_case"), "Robot block model must not reuse computer case geometry");
        assertTrue(robotItemModel.contains("minecraft:item/generated"), "Robot item model should be a flat generated item");
        assertTrue(robotItemModel.contains("neoopencomputers:item/robot"), "Robot item model should use the upstream robot item texture");
        assertTrue(!robotItemModel.contains("block/robot"), "Robot item model must not inherit the invisible block model");
        assertTrue(droneModel.contains("neoopencomputers:item/drone"), "Drone item model should use drone item texture");
        assertTrue(!droneModel.contains("item/tablet"), "Drone item model must not reuse tablet texture");
        assertTrue(droneCaseModel.contains("neoopencomputers:item/drone_case_tier1"), "Drone case model should use matching case item texture");
        assertTrue(!droneCaseModel.contains("item/tablet_case"), "Drone case item model must not reuse tablet case texture");
    }

    @Test
    void droneFoundationItemModelsExistForClientResourceReload() {
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/drone_case_tier1.json")), "Missing tier 1 drone case item model");
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/drone_case_tier2.json")), "Missing tier 2 drone case item model");
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/drone_case_creative.json")), "Missing creative drone case item model");
        assertTrue(Files.exists(MODEL_ROOT.resolve("item/drone.json")), "Missing drone item model");
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
