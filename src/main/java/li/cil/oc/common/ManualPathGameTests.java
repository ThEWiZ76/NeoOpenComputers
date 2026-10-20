package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.client.ManualContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ManualPathGameTests {
    @GameTest(template = "empty")
    public static void registeredComponentsOpenTheirBundledManualPages(GameTestHelper helper) {
        final var manual = manual();
        for (final var name : List.of("graphicscard1", "graphicscard2", "graphicscard3", "cpu1", "cpu2", "cpu3", "ram1", "ram2", "ram3", "ram4", "ram5", "ram6", "hdd1", "hdd2", "hdd3", "datacard1", "datacard2", "datacard3", "lancard", "wlancard1", "wlancard2", "redstonecard1", "redstonecard2", "inventorycontrollerupgrade", "cardcontainer1", "databaseupgrade1", "hoverupgrade1", "floppy", "eeprom")) {
            assertPath(helper, manual, "item", name);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registeredBlocksShareItemAndWorldManualPaths(GameTestHelper helper) {
        final var manual = manual();
        for (final var name : List.of("case1", "case2", "case3", "screen1", "screen2", "screen3", "diskdrive", "rack", "raid", "hologram1", "hologram2", "powerconverter", "powerdistributor")) {
            assertPath(helper, manual, "block", name);
        }
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SCREEN_TIER1.get());
        helper.assertTrue("%LANGUAGE%/block/screen1.md".equalsIgnoreCase(manual.pathFor(helper.getLevel(), helper.absolutePos(pos))), "World lookup did not use the same screen documentation as its item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upstreamExcludedDefinitionsDoNotExposeBrokenShortcuts(GameTestHelper helper) {
        final var manual = manual();
        for (final var item : List.of(ModItems.DEBUGGER.get(), ModItems.DIAMOND_CHIP.get(), ModItems.ENDSTONE.get())) {
            helper.assertTrue(manual.pathFor(new ItemStack(item)) == null, "Upstream-excluded item still exposes a missing manual shortcut");
        }
        helper.assertTrue(manual.pathFor(ItemStack.EMPTY) == null && manual.pathFor(new ItemStack(net.minecraft.world.item.Items.PAPER)) == null, "Unregistered item acquired a manual shortcut");
        final var previous = API.items;
        try {
            final var custom = new ItemRegistry();
            custom.register("pack_defined_manual", null, net.minecraft.world.item.Items.PAPER);
            API.items = custom;
            helper.assertTrue("%LANGUAGE%/item/pack_defined_manual.md".equals(manual.pathFor(new ItemStack(net.minecraft.world.item.Items.PAPER))), "Missing bundled page prevented a custom content provider from supplying it");
        } finally {
            API.items = previous;
        }
        helper.succeed();
    }

    private static ManualRegistry manual() {
        final var manual = new ManualRegistry();
        ManualContent.registerDefaults(manual);
        return manual;
    }

    private static void assertPath(GameTestHelper helper, ManualRegistry manual, String kind, String name) {
        final var definition = API.items.get(name);
        helper.assertTrue(definition != null, "Missing real test definition: " + name);
        final String expected = "%LANGUAGE%/" + kind + "/" + name + ".md";
        helper.assertTrue(ManualPathGameTests.class.getResource("/assets/neoopencomputers/doc/" + expected.replace("%LANGUAGE%", "en_us")) != null, "Missing expected bundled fixture page: " + name);
        final var actual = manual.pathFor(definition.createItemStack(1));
        helper.assertTrue(expected.equalsIgnoreCase(actual), "Registered " + name + " opens missing/wrong manual page: " + actual);
    }
}
