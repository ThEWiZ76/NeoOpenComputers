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
    @GameTest(template = "empty", timeoutTicks = 220)
    public static void actualComputerCraftComputerExchangesBundledSignals(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) { helper.succeed(); return; }
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState());
        final var host = (ComputerCaseBlockEntity) world.getBlockEntity(pos);
        for (int color = 0; color < 16; color++) host.setBundledRedstoneOutput(Direction.UP, color, 255);
        final var ccPos = pos.above();
        final var cleanup = bootComputerCraft(helper, ccPos,
            "redstone.setBundledOutput('bottom', 32773)\n" +
            "while true do redstone.setBundledOutput('top', redstone.getBundledInput('bottom')); os.pullEvent('redstone') end\n");
        final var initialEcho = new java.util.concurrent.atomic.AtomicBoolean();
        helper.succeedWhen(() -> {
            helper.assertTrue(dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(world, ccPos, Direction.UP) == (initialEcho.get() ? 32767 : 65535),
                "Actual CC Lua computer did not read and echo all OC channels");
            for (int color = 0; color < 16; color++) {
                helper.assertTrue(host.bundledRedstoneInput(Direction.UP, color) == ((32773 & (1 << color)) == 0 ? 0 : 15),
                    "OC did not sample actual CC output on color " + color);
            }
            if (!initialEcho.getAndSet(true)) {
                host.setBundledRedstoneOutput(Direction.UP, 15, 0);
                helper.fail("Waiting for CC to echo the changed output");
            }
            cleanup.run();
        });
    }

    @GameTest(template = "empty")
    public static void computerCraftBundledSignalsCrossTheRegisteredApi(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            helper.assertTrue(!Boolean.getBoolean("neoopencomputers.testComputerCraft"), "CC integration run did not load CC");
            helper.succeed(); return;
        }
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var world = helper.getLevel();
        world.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState());
        final var host = (ComputerCaseBlockEntity) world.getBlockEntity(pos);
        final var stack = new ItemStack(ModItems.REDSTONE_CARD_TIER2.get());
        final var environment = API.driver.driverFor(stack).createEnvironment(stack, host);
        try {
            final var component = (Component) environment.node();
            helper.assertTrue(component.methods().contains("setBundledOutput"), "Advanced card did not enable bundled callbacks with CC installed");
            // Each bit, including bit15, must survive the real CC API provider dispatch.
            for (int color = 0; color < 16; color++) {
                component.invoke("setBundledOutput", null, Direction.UP.get3DDataValue(), color, 1);
                helper.assertTrue(ccProviderOutput(world, pos, Direction.UP) == (1 << (color + 1)) - 1,
                    "OC output lost bundled color " + color);
            }
            component.invoke("setBundledOutput", null, Direction.UP.get3DDataValue(), 15, 0);
            helper.assertTrue(ccProviderOutput(world, pos, Direction.UP) == 32767, "Clearing an output left its bit set");
            helper.assertTrue(ccProviderOutput(world, pos.below(), Direction.UP) == 0,
                "CC provider dispatch emitted power for an unrelated block");
        } finally { environment.node().remove(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void bundledIoChangesSignalColorAndMicrocontrollerUsesSameProvider(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) { helper.succeed(); return; }
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.REDSTONE_IO.get().defaultBlockState());
        final var io = (li.cil.oc.common.blockentity.RedstoneIoBlockEntity) world.getBlockEntity(pos);
        final var source = pos.above();
        final var events = new java.util.ArrayList<Object[]>();
        final var listener = new li.cil.oc.api.prefab.AbstractManagedEnvironment() {
            { setNode(li.cil.oc.api.Network.newNode(this, li.cil.oc.api.network.Visibility.Network).create()); }
            @Override public void onMessage(li.cil.oc.api.network.Message message) {
                if (message.name().equals("computer.signal")) events.add(message.data());
            }
        };
        li.cil.oc.api.Network.joinNewNetwork(io.node());
        io.node().connect(listener.node());
        io.updateRedstoneInputs();
        helper.assertTrue(events.isEmpty(), "Initial bundled sampling emitted a false change");
        final var microPos = pos.above(2);
        world.setBlockAndUpdate(microPos, ModBlocks.MICROCONTROLLER_TIER2.get().defaultBlockState());
        final var micro = (li.cil.oc.common.blockentity.MicrocontrollerBlockEntity) world.getBlockEntity(microPos);
        final var cleanup = bootComputerCraft(helper, source, "redstone.setBundledOutput('bottom', 32773); redstone.setBundledOutput('top', 32773)");
        helper.succeedWhen(() -> {
            helper.assertTrue(io.bundledRedstoneInput(Direction.UP, 15) == 15 && micro.bundledRedstoneInput(Direction.DOWN, 15) == 15,
                "Both hosts must receive actual CC computer output");
            try {
                helper.assertTrue(events.size() == 3, "Expected exactly three changed colors");
                for (int index = 0; index < 3; index++) {
                    final Object[] event = events.get(index);
                    helper.assertTrue(java.util.Arrays.equals(event, new Object[]{"redstone_changed", Direction.UP.get3DDataValue(), 0, 15, new int[]{0, 2, 15}[index]}),
                        "Bundled signal lost side, values or color");
                }
                events.clear();
                io.updateRedstoneInputs();
                helper.assertTrue(events.isEmpty(), "Unchanged bundled inputs emitted duplicate events");
                helper.assertTrue(((Number) ((Component) io.node()).invoke("getBundledInput", null, 1, 15)[0]).intValue() == 15, "Redstone IO callback still returned constant zero");
                io.setBundledRedstoneOutput(Direction.WEST, 15, 1);
                helper.assertTrue(ccProviderOutput(world, pos, Direction.WEST) == 32768, "Redstone IO host did not expose output to CC");
                helper.assertTrue(micro.bundledRedstoneInput(Direction.NORTH, 15) == 0, "Microcontroller read output from the wrong face");
                micro.setBundledRedstoneOutput(Direction.SOUTH, 4, 42);
                helper.assertTrue(ccProviderOutput(world, microPos, Direction.SOUTH) == 16, "Microcontroller output did not reach CC provider dispatch");
                cleanup.run();
                io.updateRedstoneInputs();
                helper.assertTrue(events.size() == 3 && io.bundledRedstoneInput(Direction.UP, 15) == 0, "Falling inputs were not cleared/signalled");
            } catch (Exception exception) { throw new RuntimeException(exception); }
            finally { listener.node().remove(); io.node().remove(); }
        });
    }

    static int ccProviderOutput(net.minecraft.world.level.Level world, BlockPos pos, Direction side) throws Exception {
        // Only the test inspects CC's dispatch: its public getBundledRedstoneOutput intentionally reads CC blocks alone.
        return (Integer) Class.forName("dan200.computercraft.impl.BundledRedstone")
            .getMethod("getOutput", net.minecraft.world.level.Level.class, BlockPos.class, Direction.class).invoke(null, world, pos, side);
    }

    static Runnable bootComputerCraft(GameTestHelper helper, BlockPos pos, String script) throws Exception {
        return ComputerCraftFixture.boot(helper, pos, script);
    }

    // Keep CC-typed lambda captures off the GameTest class inspected by NeoForge when CC is absent.
    private static final class ComputerCraftFixture {
        private static Runnable boot(GameTestHelper helper, BlockPos pos, String script) throws Exception {
            final var world = helper.getLevel();
            world.setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("computercraft", "computer_normal")).defaultBlockState());
            final var cc = world.getBlockEntity(pos);
            final var computer = cc.getClass().getMethod("createServerComputer").invoke(cc);
            final var mount = (dan200.computercraft.api.filesystem.WritableMount) computer.getClass().getMethod("createRootMount").invoke(computer);
            try (var channel = mount.openFile("startup.lua", java.util.Set.of(java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.WRITE))) {
                final var buffer = java.nio.ByteBuffer.wrap(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) channel.write(buffer);
            }
            computer.getClass().getMethod("turnOn").invoke(computer);
            final var cleaned = new java.util.concurrent.atomic.AtomicBoolean();
            final Runnable cleanup = () -> {
                if (cleaned.getAndSet(true)) return;
                try { mount.delete("startup.lua"); } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
                world.removeBlock(pos, false);
            };
            helper.runAtTickTime(210, cleanup);
            return cleanup;
        }
    }

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
            final var expectedMethods = new java.util.HashSet<>(java.util.Set.of("getInput", "getOutput", "setOutput", "getComparatorInput", "getWakeThreshold", "setWakeThreshold"));
            if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) expectedMethods.addAll(java.util.Set.of("getBundledInput", "getBundledOutput", "setBundledOutput"));
            helper.assertTrue(component.methods().equals(expectedMethods), "Advanced card callbacks do not match installed integrations");
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
        helper.assertTrue(tab.getDisplayItems().stream().anyMatch(candidate -> candidate.getItem() == stack.getItem()) == net.neoforged.fml.ModList.get().isLoaded("computercraft"), "Advanced Creative visibility does not match bundled integration");
        helper.succeed();
    }
}
