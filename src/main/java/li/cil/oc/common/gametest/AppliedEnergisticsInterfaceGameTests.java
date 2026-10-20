package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder(NeoOpenComputers.MODID)
public final class AppliedEnergisticsInterfaceGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> interfaceTests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("ae2")) return List.of();
        return List.of(new TestFunction("ae2", "ae2_block_interface_config", "neoopencomputers:empty", 300, 0, true, Loaded::blockInterface),
            new TestFunction("ae2", "ae2_part_configs", "neoopencomputers:empty", 300, 0, true, Loaded::partConfigs),
            new TestFunction("ae2", "ae2_native_interface", "neoopencomputers:empty", 500, 0, true, Loaded::nativeInterface));
    }

    private static final class Loaded {
        static void blockInterface(final GameTestHelper helper) {
            helper.setBlock(new BlockPos(1, 1, 1), BuiltInRegistries.BLOCK.get(id("interface")));
            final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            final var environment = environment(helper, pos, "me_interface");
            final var component = (li.cil.oc.api.network.Component) environment.node();
            final var database = new li.cil.oc.common.component.DatabaseEnvironment(2);
            final var stack = new ItemStack(Items.STONE, 12);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Interface Fixture"));
            database.setPhysicalStackInSlot(0, stack);
            li.cil.oc.api.Network.joinNewNetwork(environment.node()); environment.node().connect(database.node());
            final var pauses = new java.util.ArrayList<Double>();
            final var context = (li.cil.oc.api.machine.Context) java.lang.reflect.Proxy.newProxyInstance(li.cil.oc.api.machine.Context.class.getClassLoader(),
                new Class<?>[]{li.cil.oc.api.machine.Context.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("pause")) { pauses.add((Double) arguments[0]); return true; }
                    return null;
                });
            try {
                helper.assertTrue(li.cil.oc.api.Driver.environmentsFor(new ItemStack(BuiltInRegistries.ITEM.get(id("interface")))).stream()
                    .anyMatch(type -> type.getName().contains("Interface")), "Block interface lacks item EnvironmentProvider");
                component.invoke("setInterfaceConfiguration", context, database.node().address(), 1, 128);
                final var value = describe(component.invoke("getInterfaceConfiguration", null)[0]);
                helper.assertTrue(value.get("name").equals("minecraft:stone") && ((Number) value.get("size")).intValue() == 64
                    && value.get("label").equals("Interface Fixture"), "Interface default overload lost descriptor or size clamp");
                final var config = ((appeng.helpers.IConfigInvHost) helper.getLevel().getBlockEntity(pos)).getConfig();
                helper.assertTrue(config.getAmount(0) == 64 && config.getKey(0).equals(appeng.api.stacks.AEItemKey.of(stack)), "Callback did not change real AE2 ghost config");
                helper.assertTrue(database.getStackInSlot(0).getCount() == 12 && database.getPhysicalStackInSlot(0).getCount() == 12, "Interface configuration mutated database ownership/count");
                expectError(helper, component, "setInterfaceConfiguration", "invalid slot", 0, database.node().address(), 1);
                expectError(helper, component, "setInterfaceConfiguration", "invalid slot", 1, database.node().address(), 0);
                expectError(helper, component, "setInterfaceConfiguration", "no such component", 1, "missing-database", 1);
                helper.assertTrue(config.getAmount(0) == 64, "Rejected configuration mutated filter");
                component.invoke("setInterfaceConfiguration", context, 2, database.node().address(), 1);
                helper.assertTrue(config.getAmount(1) == 1, "Explicit slot/default size mismatch");
                component.invoke("setInterfaceConfiguration", context, database.node().address(), 1, 0);
                helper.assertTrue(config.getKey(0) == null, "Zero size did not clear interface filter");
                component.invoke("setInterfaceConfiguration", context, 2);
                helper.assertTrue(config.getKey(1) == null, "Slot-only call did not clear filter");
                helper.assertTrue(pauses.equals(List.of(0.5, 0.5, 0.5, 0.5)), "Interface configuration pause contract mismatch");
                component.invoke("setInterfaceConfiguration", null, 3, database.node().address(), 1, 7);
                final var tag = helper.getLevel().getBlockEntity(pos).saveWithFullMetadata(helper.getLevel().registryAccess());
                final var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos), tag, helper.getLevel().registryAccess());
                helper.assertTrue(restored instanceof appeng.helpers.IConfigInvHost && ((appeng.helpers.IConfigInvHost) restored).getConfig().getAmount(2) == 7,
                    "Block interface ghost configuration did not survive cold NBT load");
                restored.setRemoved();
            } catch (RuntimeException exception) { throw exception; }
            catch (Exception exception) { helper.fail(exception.toString()); }
            finally { environment.node().remove(); database.node().remove(); }
            helper.succeed();
        }

        static void partConfigs(final GameTestHelper helper) {
            for (final String kind : List.of("import", "export")) {
                final var pos = helper.absolutePos(new BlockPos(kind.equals("import") ? 1 : 3, 1, 3));
                part(helper, pos, null, "fluix_glass_cable"); part(helper, pos, Direction.NORTH, kind + "_bus");
                final var single = environment(helper, pos, "me_" + kind + "bus");
                final var methods = ((li.cil.oc.api.network.Component) single.node()).methods();
                helper.assertTrue(methods.contains(kind.equals("import") ? "getImportConfiguration" : "getExportConfiguration")
                    && !methods.contains(kind.equals("import") ? "getExportConfiguration" : "getImportConfiguration")
                    && !methods.contains("getCpus"), "Single bus exposed another device's callbacks");
                single.node().remove();
            }
            final var panelPos = helper.absolutePos(new BlockPos(1, 1, 1));
            final var busPos = helper.absolutePos(new BlockPos(3, 1, 1));
            part(helper, panelPos, null, "fluix_glass_cable"); part(helper, busPos, null, "fluix_glass_cable");
            for (final var side : Direction.values()) {
                part(helper, panelPos, side, "cable_interface");
                part(helper, busPos, side, side.ordinal() % 2 == 0 ? "import_bus" : "export_bus");
            }
            final var panels = environment(helper, panelPos, "me_interface");
            final var buses = environment(helper, busPos, "me_exportbus");
            final var database = new li.cil.oc.common.component.DatabaseEnvironment(1); database.setStackInSlot(0, new ItemStack(Items.STONE, 17));
            li.cil.oc.api.Network.joinNewNetwork(panels.node()); panels.node().connect(database.node()); panels.node().connect(buses.node());
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(((appeng.api.parts.IPartHost) helper.getLevel().getBlockEntity(panelPos)).getPart(Direction.NORTH).getGridNode() != null,
                "Waiting for real interface-part grid node")).thenExecute(() -> {
              try {
                final var panelComponent = (li.cil.oc.api.network.Component) panels.node();
                final var busComponent = (li.cil.oc.api.network.Component) buses.node();
                helper.assertTrue(panelComponent.methods().containsAll(List.of("getCpus", "getCraftables", "getItemsInNetwork", "getFluidsInNetwork", "getEnergyDemand", "store")), "Panel lacks inherited network control");
                helper.assertTrue(((Object[]) panelComponent.invoke("getItemsInNetwork", null)[0]).length == 0, "Panel did not query real grid storage");
                for (final var side : Direction.values()) {
                    final int n = side.get3DDataValue();
                    panelComponent.invoke("setInterfaceConfiguration", null, n, database.node().address(), 1, n + 1);
                    helper.assertTrue(((Number) describe(panelComponent.invoke("getInterfaceConfiguration", null, n)[0]).get("size")).intValue() == n + 1, "Panel configuration side mismatch");
                    final var prefix = side.ordinal() % 2 == 0 ? "Import" : "Export";
                    busComponent.invoke("set" + prefix + "Configuration", null, n, 1, database.node().address(), 1);
                    helper.assertTrue(describe(busComponent.invoke("get" + prefix + "Configuration", null, n)[0]).get("name").equals("minecraft:stone"), "Bus filter identity missing");
                    final var actual = ((appeng.helpers.IConfigInvHost) ((appeng.api.parts.IPartHost) helper.getLevel().getBlockEntity(busPos)).getPart(side)).getConfig();
                    helper.assertTrue(actual.getKey(0).equals(appeng.api.stacks.AEItemKey.of(Items.STONE)), "Wrong actual bus ghost filter");
                    final var wrong = busComponent.invoke("get" + (prefix.equals("Import") ? "Export" : "Import") + "Configuration", null, n);
                    helper.assertTrue(wrong[0] == null && wrong[1].equals("no matching part"), "Configuration crossed into another part kind");
                }
                for (final String item : List.of("cable_interface", "import_bus", "export_bus")) helper.assertTrue(!li.cil.oc.api.Driver.environmentsFor(new ItemStack(BuiltInRegistries.ITEM.get(id(item)))).isEmpty(), "Part EnvironmentProvider missing: " + item);
                final var compound = (li.cil.oc.common.driver.CompoundBlockEnvironment) buses;
                final var addresses = compound.environments().stream().map(entry -> entry.environment().node().address()).toList();
                final var saved = new net.minecraft.nbt.CompoundTag(); compound.save(saved);
                compound.node().remove();
                final var restoredEnvironment = li.cil.oc.api.Driver.driverFor(helper.getLevel(), busPos, Direction.SOUTH).createEnvironment(helper.getLevel(), busPos, Direction.SOUTH);
                try {
                    restoredEnvironment.load(saved);
                    li.cil.oc.api.Network.joinNewNetwork(restoredEnvironment.node());
                    final var restoredAddresses = ((li.cil.oc.common.driver.CompoundBlockEnvironment) restoredEnvironment).environments().stream().map(entry -> entry.environment().node().address()).toList();
                    helper.assertTrue(addresses.equals(restoredAddresses), "Mixed-bus component save/load collided driver identities");
                } finally { restoredEnvironment.node().remove(); }
                final var busTag = helper.getLevel().getBlockEntity(busPos).saveWithFullMetadata(helper.getLevel().registryAccess());
                final var restoredHost = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(busPos, helper.getLevel().getBlockState(busPos), busTag, helper.getLevel().registryAccess());
                for (final var side : Direction.values()) helper.assertTrue(((appeng.helpers.IConfigInvHost) ((appeng.api.parts.IPartHost) restoredHost).getPart(side)).getConfig().getKey(0)
                    .equals(appeng.api.stacks.AEItemKey.of(Items.STONE)), "Multipart filter lost during cold NBT load");
                restoredHost.setRemoved();
                expectError(helper, panelComponent, "getInterfaceConfiguration", "invalid side", 6);
                expectError(helper, busComponent, "getExportConfiguration", "invalid side", -1);
                ((appeng.api.parts.IPartHost) helper.getLevel().getBlockEntity(panelPos)).removePartFromSide(Direction.NORTH);
                final var missing = panelComponent.invoke("getInterfaceConfiguration", null, Direction.NORTH.get3DDataValue());
                helper.assertTrue(missing[0] == null && missing[1].equals("no matching part"), "Removed panel still exposes configuration");
                helper.assertTrue(database.getStackInSlot(0).getCount() == 17 && database.getPhysicalStackInSlot(0).isEmpty(), "Part configuration materialized database descriptions");
              } catch (RuntimeException exception) { throw exception; }
              catch (Exception exception) { helper.fail(exception.toString()); }
              finally { panels.node().remove(); buses.node().remove(); database.node().remove(); }
            }).thenSucceed();
        }

        static void nativeInterface(final GameTestHelper helper) {
            helper.setBlock(new BlockPos(1, 1, 1), BuiltInRegistries.BLOCK.get(id("interface")));
            final var tile = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
            ((appeng.helpers.IConfigInvHost) tile).getConfig().setStack(0, new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.STONE), 5));
            helper.setBlock(new BlockPos(1, 1, 2), li.cil.oc.common.ModBlocks.ADAPTER.get());
            helper.setBlock(new BlockPos(1, 1, 3), li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(new BlockPos(1, 1, 2));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 3));
            final var cpu = new ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local iface = component.proxy(assert(component.list('me_interface')(), 'adapter did not expose me_interface'))
                local config = iface.getInterfaceConfiguration()
                assert(config.name == 'minecraft:stone' and config.size == 5)
                local items, why = iface.getItemsInNetwork()
                assert(type(items) == 'table', 'items query: ' .. tostring(why))
                local recipes, why = iface.getCraftables()
                assert(type(recipes) == 'table', 'recipes query: ' .. tostring(why))
                assert(type(iface.getCpus()) == 'table' and type(iface.getStoredPower()) == 'number')
                assert(iface.setInterfaceConfiguration(1) == true)
                assert(iface.getInterfaceConfiguration() == nil)
                component.proxy(component.list('eeprom')()).setData('ae2-interface-ok')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors(); li.cil.oc.api.Network.joinOrCreateNetwork(adapter); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            li.cil.oc.NeoOpenComputers.LOGGER.debug("Native AE2 interface fixture internal node ready before wait: {}", ((appeng.api.networking.security.IActionHost) tile).getActionableNode() != null);
            helper.startSequence().thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, tile.getBlockPos(), null);
                helper.assertTrue(host != null && ((appeng.api.networking.security.IActionHost) tile).getActionableNode() != null, "Waiting for native interface grid node");
            }).thenExecute(() -> helper.assertTrue(computer.machine().start(), "Native interface computer did not start"))
                .thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(
                    li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("ae2-interface-ok"), "Native interface program failed: " + computer.machine().lastError())).thenSucceed();
        }

        static li.cil.oc.api.network.ManagedEnvironment environment(final GameTestHelper helper, final BlockPos pos, final String expected) {
            final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.SOUTH);
            helper.assertTrue(driver != null, "No driver for real AE2 " + expected);
            final var environment = driver.createEnvironment(helper.getLevel(), pos, Direction.SOUTH);
            helper.assertTrue(((li.cil.oc.api.network.Component) environment.node()).name().equals(expected), "Wrong AE2 component name; expected " + expected);
            return environment;
        }

        static void part(final GameTestHelper helper, final BlockPos pos, final Direction side, final String item) {
            helper.assertTrue(appeng.api.parts.PartHelper.setPart(helper.getLevel(), pos, side, null,
                (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(id(item))) != null, "Failed placing actual AE2 part " + item);
        }

        static Map<?, ?> describe(final Object value) { return (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{value})[0]; }
        static ResourceLocation id(final String name) { return ResourceLocation.fromNamespaceAndPath("ae2", name); }
        static void expectError(final GameTestHelper helper, final li.cil.oc.api.network.Component component, final String method, final String message, final Object... args) throws Exception {
            try { component.invoke(method, null, args); }
            catch (IllegalArgumentException exception) { helper.assertTrue(message.equals(exception.getMessage()), "Wrong config error: " + exception.getMessage()); return; }
            helper.fail("Expected " + message);
        }
    }
}
