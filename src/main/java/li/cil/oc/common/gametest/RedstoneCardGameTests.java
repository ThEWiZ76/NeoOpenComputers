package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneCardGameTests {
    @GameTest(template = "empty")
    public static void basicRedstoneCardDoesNotExposeBundledCallbacks(GameTestHelper helper) throws Exception {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var host = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var stack = new ItemStack(ModItems.REDSTONE_CARD.get());
        final var driver = API.driver.driverFor(stack, host.getClass());
        final var environment = driver.createEnvironment(stack, host);
        try {
            final var component = (Component) environment.node();
            helper.assertTrue(!component.methods().contains("getBundledInput") && !component.methods().contains("getBundledOutput")
                && !component.methods().contains("setBundledOutput"), "Tier-one card exposes bundled callbacks");
            component.invoke("setOutput", null, Direction.UP.get3DDataValue(), 15);
            helper.assertTrue(host.redstoneOutput(Direction.UP) == 15, "Vanilla redstone output stopped working");
            try {
                component.invoke("setBundledOutput", null, 1, 0, 255);
                helper.fail("Hidden bundled callback remained invokable");
            } catch (NoSuchMethodException expected) {
                // A hidden callback must also reject component.invoke.
            }
        } finally { environment.node().remove(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancedRedstoneCardHasTierRecipeAndVanillaFallback(GameTestHelper helper) throws Exception {
        final var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "redstone_card_tier2"));
        helper.assertTrue(item.isPresent(), "Tier-two redstone card is missing");
        final var stack = new ItemStack(item.orElseThrow());
        helper.assertTrue(API.items.get("redstonecard2") != null && API.items.get("redstonecard2").createItemStack(1).getItem() == stack.getItem(), "Upstream tier-two API name is missing");
        final var driver = API.driver.driverFor(stack);
        helper.assertTrue(driver != null && driver.tier(stack) == 1 && li.cil.oc.api.driver.item.Slot.Card.equals(driver.slot(stack)), "Advanced card does not have tier-two slot/driver metadata");
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var basic = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(!basic.canPlaceItem(ComputerCaseBlockEntity.SLOT_CARD_0, stack), "Tier-one case accepted the advanced card");
        final var advancedPos = pos.above(2);
        helper.getLevel().setBlockAndUpdate(advancedPos, ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState());
        final var advanced = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(advancedPos);
        helper.assertTrue(advanced.canPlaceItem(0, stack) && !advanced.canPlaceItem(1, stack), "Tier-two case did not enforce mixed card-slot tiers");
        final var environment = driver.createEnvironment(stack, advanced);
        try {
            final var component = (Component) environment.node();
            helper.assertTrue(component.methods().equals(java.util.Set.of("getInput", "getOutput", "setOutput", "getComparatorInput", "getWakeThreshold", "setWakeThreshold")),
                "No-integration advanced card does not use vanilla fallback");
            component.invoke("setOutput", null, Direction.UP.get3DDataValue(), 12);
            helper.assertTrue(advanced.redstoneOutput(Direction.UP) == 12, "Advanced fallback did not drive actual host redstone");
            li.cil.oc.api.Network.joinNewNetwork(environment.node());
            final String address = environment.node().address();
            environment.save(new net.minecraft.nbt.CompoundTag());
            environment.node().remove();
            final var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (net.minecraft.nbt.CompoundTag) stack.save(helper.getLevel().registryAccess()));
            final var loadedDriver = API.driver.driverFor(loaded);
            helper.assertTrue(loadedDriver.tier(loaded) == 1, "Card tier changed after item serialization");
            final var restored = loadedDriver.createEnvironment(loaded, advanced);
            try {
                helper.assertTrue(address.equals(restored.node().address()), "Advanced card lost its component address in item data");
            } finally { restored.node().remove(); }
        } finally { environment.node().remove(); }
        final var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "redstone_card_tier2"));
        helper.assertTrue(recipe.isPresent(), "Advanced card recipe is missing");
        @SuppressWarnings("unchecked")
        final var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) recipe.orElseThrow().value();
        final var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(
            new ItemStack(net.minecraft.world.item.Items.REDSTONE_BLOCK), new ItemStack(ModItems.MICROCHIP_TIER2.get()), new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),
            ItemStack.EMPTY, new ItemStack(ModItems.CARD.get()), ItemStack.EMPTY));
        helper.assertTrue(crafting.matches(input, helper.getLevel()) && crafting.assemble(input, helper.getLevel().registryAccess()).getItem() == stack.getItem(), "Advanced card cannot be crafted from upstream ingredients");
        helper.assertTrue(li.cil.oc.common.item.PresentLoot.eligible(helper.getLevel()).stream().anyMatch(entry -> entry.item().get() == stack.getItem() && entry.weight() == 17), "Advanced card missing from craftable present loot");
        final var unsupported = new li.cil.oc.api.network.EnvironmentHost() {
            public net.minecraft.world.level.Level world() { return helper.getLevel(); }
            public double xPosition() { return 0; }
            public double yPosition() { return 0; }
            public double zPosition() { return 0; }
            public void markChanged() {}
        };
        helper.assertTrue(driver.createEnvironment(stack, unsupported) == null, "No-integration card created a broken component on an unsupported host");
        final var tab = li.cil.oc.common.ModCreativeTabs.MAIN.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().noneMatch(candidate -> candidate.getItem() == stack.getItem()), "Advanced card exposed in Creative without bundled integration");
        helper.succeed();
    }
}
