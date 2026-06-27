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
    void screenBlockModelsUseGenericCaseModelLikeUpstreamScreenModel() throws IOException {
        try (Reader reader = Files.newBufferedReader(BLOCK_MODEL_ROOT.resolve("screen_panel.json"))) {
            final JsonObject panel = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue("minecraft:block/cube_bottom_top".equals(panel.get("parent").getAsString()));
            assertTrue(!panel.has("elements"));
            final JsonObject textures = panel.getAsJsonObject("textures");
            assertTrue("neoopencomputers:block/generic_top".equals(textures.get("bottom").getAsString()));
            assertTrue("neoopencomputers:block/generic_top".equals(textures.get("top").getAsString()));
            assertTrue("neoopencomputers:block/generic_side".equals(textures.get("side").getAsString()));
        }

        final List<String> modelNames = List.of("screen_tier1.json", "screen_tier2.json", "screen_tier3.json");
        for (final String modelName : modelNames) {
            try (Reader reader = Files.newBufferedReader(BLOCK_MODEL_ROOT.resolve(modelName))) {
                final JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue("neoopencomputers:block/screen_panel".equals(model.get("parent").getAsString()), modelName);
                assertTrue(!model.has("textures"), modelName);
            }
        }

        final List<String> horizontalModelNames = List.of("screen_tier1_horizontal.json", "screen_tier2_horizontal.json", "screen_tier3_horizontal.json");
        for (final String modelName : horizontalModelNames) {
            try (Reader reader = Files.newBufferedReader(BLOCK_MODEL_ROOT.resolve(modelName))) {
                final JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue("neoopencomputers:block/screen_panel".equals(model.get("parent").getAsString()), modelName);
                assertTrue(!model.has("textures"), modelName);
            }
        }
    }

    @Test
    void horizontalScreenBlockstatesUseUpstreamPitchFrontTextureVariant() throws IOException {
        for (final String tier : List.of("screen_tier1", "screen_tier2", "screen_tier3")) {
            final Path blockstatePath = Path.of("src/main/resources/assets/neoopencomputers/blockstates/" + tier + ".json");
            try (Reader reader = Files.newBufferedReader(blockstatePath)) {
                final JsonObject variants = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("variants");
                for (final String yaw : List.of("north", "south", "east", "west")) {
                    assertTrue(variants.getAsJsonObject("pitch=north,yaw=" + yaw).get("model").getAsString().equals("neoopencomputers:block/" + tier), tier + " wall " + yaw);
                    assertTrue(variants.getAsJsonObject("pitch=up,yaw=" + yaw).get("model").getAsString().equals("neoopencomputers:block/" + tier + "_horizontal"), tier + " up " + yaw);
                    assertTrue(variants.getAsJsonObject("pitch=down,yaw=" + yaw).get("model").getAsString().equals("neoopencomputers:block/" + tier + "_horizontal"), tier + " down " + yaw);
                }
            }
        }
    }

    @Test
    void diskDriveBlockstateDoesNotChangeModelWhenMediaIsInserted() throws IOException {
        final Path blockstatePath = Path.of("src/main/resources/assets/neoopencomputers/blockstates/disk_drive.json");
        try (Reader reader = Files.newBufferedReader(blockstatePath)) {
            final JsonObject variants = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("variants");
            for (final String facing : List.of("north", "east", "south", "west")) {
                assertTrue(variants.has("facing=" + facing), facing);
                assertTrue(variants.getAsJsonObject("facing=" + facing).get("model").getAsString().equals("neoopencomputers:block/disk_drive"), facing);
                assertTrue(!variants.has("facing=" + facing + ",has_media=false"), facing);
                assertTrue(!variants.has("facing=" + facing + ",has_media=true"), facing);
            }
        }

        assertTrue(!Files.exists(BLOCK_MODEL_ROOT.resolve("disk_drive_loaded.json")));
    }
}
