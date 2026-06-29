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
                    if (textures.has("layer0") && isVanillaPlaceholder(textures.get("layer0").getAsString())) {
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

    @Test
    void floppyItemModelUsesUpstreamPerDyeOverrides() throws IOException {
        final List<String> dyeNames = List.of(
            "dyeblack",
            "dyered",
            "dyegreen",
            "dyebrown",
            "dyeblue",
            "dyepurple",
            "dyecyan",
            "dyelightgray",
            "dyegray",
            "dyepink",
            "dyelime",
            "dyeyellow",
            "dyelightblue",
            "dyemagenta",
            "dyeorange",
            "dyewhite");

        final String floppyModel = Files.readString(ITEM_MODEL_ROOT.resolve("floppy.json"));
        assertTrue(floppyModel.contains("neoopencomputers:floppy_color"));
        for (final String dyeName : dyeNames) {
            final String name = "floppy_" + dyeName;
            assertTrue(Files.exists(ITEM_MODEL_ROOT.resolve(name + ".json")), name);
            assertTrue(Files.exists(ITEM_TEXTURE_ROOT.resolve(name + ".png")), name);
            assertTrue(floppyModel.contains("neoopencomputers:item/" + name), name);
        }
    }

    @Test
    void tabletItemModelUsesUpstreamRunningStateOverrides() throws IOException {
        final String tabletModel = Files.readString(ITEM_MODEL_ROOT.resolve("tablet.json"));

        assertTrue(tabletModel.contains("neoopencomputers:tablet_running"));
        assertTrue(tabletModel.contains("neoopencomputers:item/tablet_off"));
        assertTrue(tabletModel.contains("neoopencomputers:item/tablet_on"));
        assertTrue(Files.exists(ITEM_MODEL_ROOT.resolve("tablet_off.json")));
        assertTrue(Files.exists(ITEM_MODEL_ROOT.resolve("tablet_on.json")));
        assertTrue(Files.exists(ITEM_TEXTURE_ROOT.resolve("tablet_off.png")));
        assertTrue(Files.exists(ITEM_TEXTURE_ROOT.resolve("tablet_on.png")));
    }

    @Test
    void screenItemModelsUseUpstreamBlockQuads() throws IOException {
        for (final String tier : List.of("screen_tier1", "screen_tier2", "screen_tier3")) {
            try (Reader reader = Files.newBufferedReader(ITEM_MODEL_ROOT.resolve(tier + ".json"))) {
                final JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue("neoopencomputers:block/screen_item".equals(model.get("parent").getAsString()), tier);
            }
        }
    }

    private static boolean isVanillaPlaceholder(final String texture) {
        return texture.startsWith("minecraft:item/") || texture.startsWith("minecraft:block/");
    }
}
