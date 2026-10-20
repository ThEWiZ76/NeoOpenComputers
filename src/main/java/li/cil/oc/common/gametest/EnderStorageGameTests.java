package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
public final class EnderStorageGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> enderStorageTests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("enderstorage")) {
            if (Boolean.getBoolean("neoopencomputers.testEnderStorage")) throw new IllegalStateException("Requested EnderStorage fixture is missing");
            return List.of();
        }
        return List.of(
            new TestFunction("enderStorage", "enderstorage_frequency_contract", "neoopencomputers:empty", 200, 0, true, Loaded::frequencyContract),
            new TestFunction("enderStorage", "enderstorage_native_adapter", "neoopencomputers:empty", 400, 0, true, Loaded::nativeAdapter));
    }

    private static final class Loaded {
        static void frequencyContract(final GameTestHelper helper) {
            try {
                for (final String kind : List.of("ender_chest", "ender_tank")) {
                    final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(kind.equals("ender_chest") ? 1 : 3, 1, 1));
                    final var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("enderstorage", kind));
                    helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
                    final var tile = (codechicken.enderstorage.tile.TileFrequencyOwner) helper.getLevel().getBlockEntity(pos);
                    final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, net.minecraft.core.Direction.NORTH);
                    helper.assertTrue(driver != null, "No OC driver for real EnderStorage " + kind);
                    final var environment = driver.createEnvironment(helper.getLevel(), pos, net.minecraft.core.Direction.NORTH);
                    try {
                        final var component = (li.cil.oc.api.network.Component) environment.node();
                        helper.assertTrue(component.methods().containsAll(List.of("getFrequency", "setFrequency", "getOwner", "getFrequencyColors", "getColors")),
                            "EnderStorage driver lacks frequency callbacks for " + kind);
                        helper.assertTrue(component.name().equals(kind), "Wrong EnderStorage preferred component name: " + component.name());
                        final var owner = java.util.UUID.fromString("73100000-0000-0000-0000-000000000001");
                        tile.setFreq(new codechicken.enderstorage.api.Frequency(codechicken.lib.colour.EnumColour.WHITE,
                            codechicken.lib.colour.EnumColour.WHITE, codechicken.lib.colour.EnumColour.WHITE,
                            java.util.Optional.of(owner), java.util.Optional.of(net.minecraft.network.chat.Component.literal("Fixture Owner"))));
                        component.invoke("setFrequency", null, 0xABC);
                        helper.assertTrue(java.util.Arrays.equals((Object[]) component.invoke("getFrequency", null)[0], new Integer[]{10, 11, 12}),
                            "Packed frequency digits did not roundtrip");
                        helper.assertTrue(tile.getFrequency().owner().equals(java.util.Optional.of(owner))
                            && tile.getFrequency().ownerName().orElseThrow().getString().equals("Fixture Owner"), "Frequency change lost private ownership");
                        helper.assertTrue(component.invoke("getOwner", null)[0].equals("Fixture Owner"), "Owner name callback mismatch");
                        final var colors = (java.util.Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(component.invoke("getColors", null))[0];
                        helper.assertTrue(colors.size() == 16 && colors.get(0).equals("WHITE") && colors.get(15).equals("BLACK"), "Color map lost zero-based keys or names");
                        for (int color = 0; color < 16; color++) {
                            component.invoke("setFrequency", null, color, 15 - color, color);
                            helper.assertTrue(java.util.Arrays.equals((Object[]) component.invoke("getFrequency", null)[0], new Integer[]{color, 15 - color, color}),
                                "Color index mapping is not reversible: " + color);
                            final var named = (Object[]) li.cil.oc.common.OpenComputersApi.convert(component.invoke("getFrequencyColors", null))[0];
                            helper.assertTrue(java.util.Arrays.equals(named, new Object[]{colors.get(color), colors.get(15 - color), colors.get(color)}),
                                "Frequency color names disagree with color map");
                        }
                        final var before = tile.getFrequency();
                        for (final Object[] invalid : List.of(new Object[]{-1}, new Object[]{4096}, new Object[]{-1, 0, 0}, new Object[]{0, 16, 0}, new Object[]{0, 0, 16})) {
                            boolean rejected = false;
                            try { component.invoke("setFrequency", null, invalid); }
                            catch (IllegalArgumentException exception) { rejected = exception.getMessage().equals("invalid frequency"); }
                            helper.assertTrue(rejected && tile.getFrequency().equals(before), "Invalid frequency mutated private channel");
                        }
                        final var saved = tile.saveWithFullMetadata(helper.getLevel().registryAccess());
                        final var restored = (codechicken.enderstorage.tile.TileFrequencyOwner) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                            pos, tile.getBlockState(), saved, helper.getLevel().registryAccess());
                        helper.assertTrue(restored != null && restored.getFrequency().equals(before), "EnderStorage save/load changed frequency/owner");
                        restored.setRemoved();
                        tile.setFreq(new codechicken.enderstorage.api.Frequency(before.left(), before.middle(), before.right(),
                            java.util.Optional.of(owner), java.util.Optional.empty()));
                        helper.assertTrue(component.invoke("getOwner", null)[0].equals(owner.toString()), "UUID-only ownership lost its identity");
                        component.invoke("setFrequency", null, 0x123);
                        helper.assertTrue(tile.getFrequency().owner().equals(java.util.Optional.of(owner)) && tile.getFrequency().ownerName().isEmpty(),
                            "UUID-only frequency change invented or lost owner metadata");
                        tile.setFreq(before.withoutOwner());
                        helper.assertTrue(component.invoke("getOwner", null)[0].equals("global"), "Public frequency did not report global owner");
                    } finally { environment.node().remove(); }
                }
                helper.succeed();
            } catch (Exception exception) { throw new IllegalStateException(exception); }
        }

        static void nativeAdapter(final GameTestHelper helper) {
            helper.setBlock(new net.minecraft.core.BlockPos(1, 1, 1), net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("enderstorage", "ender_chest")));
            helper.setBlock(new net.minecraft.core.BlockPos(1, 1, 2), li.cil.oc.common.ModBlocks.ADAPTER.get());
            helper.setBlock(new net.minecraft.core.BlockPos(1, 1, 3), li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(new net.minecraft.core.BlockPos(1, 1, 2));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new net.minecraft.core.BlockPos(1, 1, 3));
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4, cpu);
            computer.setItem(2, new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local address = component.list('ender_chest')()
                assert(address, 'adapter did not expose ender_chest')
                local storage = component.proxy(address)
                assert(storage.getOwner() == 'global')
                assert(select('#', storage.setFrequency(0x123)) == 0)
                local digits = storage.getFrequency()
                assert(digits[1] == 1 and digits[2] == 2 and digits[3] == 3)
                local colors = storage.getColors()
                local named = storage.getFrequencyColors()
                assert(type(colors[0]) == 'string' and named[1] == colors[1] and named[3] == colors[3])
                local ok, reason = pcall(storage.setFrequency, 4096)
                assert(not ok and tostring(reason):find('invalid frequency', 1, true))
                component.proxy(component.list('eeprom')()).setData('enderstorage-ok')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors();
            li.cil.oc.api.Network.joinOrCreateNetwork(adapter);
            li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(1000);
            helper.assertTrue(computer.machine().start(), "Native EnderStorage computer did not start");
            helper.succeedWhen(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6))
                .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("enderstorage-ok"),
                "Native adapter program failed: " + computer.machine().lastError()));
        }
    }
}
