package li.cil.oc.common.gametest;

import li.cil.oc.api.API;
import li.cil.oc.client.ItemStackImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.client.OreDictImageProvider;
import li.cil.oc.common.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ManualImageGameTests {
    @GameTest(template = "empty")
    public static void legacyContentImagesResolveBlocksItemsAndCamelCaseAliases(GameTestHelper helper) {
        final var provider = new OreDictImageProvider();
        final var names = new String[]{"oc:raid", "oc:cpu1", "oc:graphicsCard1", "oc:materialALU"};
        final var expected = new net.minecraft.world.item.Item[]{ModItems.RAID.get(), ModItems.CPU_TIER1.get(), ModItems.GRAPHICS_CARD_TIER1.get(), ModItems.ALU.get()};
        for (int index = 0; index < names.length; index++) {
            final var image = provider.getImage(names[index]);
            helper.assertTrue(image instanceof ItemStackImageRenderer, "Manual content alias rendered missing: " + names[index]);
            final var stacks = ((ItemStackImageRenderer) image).stacks();
            helper.assertTrue(stacks.size() == 1 && stacks.getFirst().is(expected[index]), "Manual alias displayed the wrong registered item: " + names[index]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void specialStackImagesRetainRegisteredComponents(GameTestHelper helper) {
        final var expected = API.items.get("openos").createItemStack(1);
        final var image = new OreDictImageProvider().getImage("oc:openos");
        helper.assertTrue(image instanceof ItemStackImageRenderer && ItemStack.isSameItemSameComponents(((ItemStackImageRenderer) image).stacks().getFirst(), expected), "Manual special stack lost its registered factory/components");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void modernTagImagesAndMissingWarningsRemainAvailable(GameTestHelper helper) {
        final var previous = API.items;
        try {
            API.items = null;
            final var image = new OreDictImageProvider().getImage("minecraft:logs");
            final var expected = BuiltInRegistries.ITEM.getTag(ItemTags.LOGS).orElseThrow();
            helper.assertTrue(image instanceof ItemStackImageRenderer && ((ItemStackImageRenderer) image).stacks().size() == expected.size() && expected.size() > 1, "Modern tag image lost its cycling ingredients");
            for (String name : new String[]{"oc:missing-fixture", "not a tag name", ""}) {
                final var missing = new OreDictImageProvider().getImage(name);
                helper.assertTrue(missing instanceof InteractiveImageRenderer && ((InteractiveImageRenderer) missing).getTooltip("").equals("oc:gui.Manual.Warning.OreDictMissing"), "Missing manual image no longer warns");
            }
        } finally { API.items = previous; }
        helper.succeed();
    }
}
