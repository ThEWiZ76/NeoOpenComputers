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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
public final class AppliedEnergisticsExportGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> exportTests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("ae2")) return List.of();
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            final var tests = new ArrayList<TestFunction>();
            for (final var side : Direction.values()) tests.add(new TestFunction("ae2Export", "ae2_export_" + side.getName(), "neoopencomputers:ae2_export", 400, 0, true,
                helper -> directionContract(helper, side)));
            tests.add(new TestFunction("ae2Export", "ae2_export_fuzzy", "neoopencomputers:ae2_export", 400, 0, true, Loaded::fuzzy));
            tests.add(new TestFunction("ae2Export", "ae2_export_native", "neoopencomputers:ae2_export", 600, 0, true, Loaded::nativeExport));
            tests.add(new TestFunction("ae2Export", "ae2_export_commit_recovery", "neoopencomputers:ae2_export", 400, 0, true, Loaded::commitRecovery));
            tests.add(new TestFunction("ae2Export", "ae2_export_unloaded_target", "neoopencomputers:empty", 200, 0, true, Loaded::unloaded));
            return tests;
        }

        static void directionContract(final GameTestHelper helper, final Direction side) {
            final var f = new Fixture(helper, side);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(f.bus.getGridNode() != null, "Waiting for export node"))
                .thenWaitUntil(f::awaitPoweredGrid)
                .thenExecute(() -> {
                  try {
                    final var component = (li.cil.oc.api.network.Component) f.environment.node();
                    helper.assertTrue(component.methods().contains("exportIntoSlot"), "Export bus lacks exportIntoSlot");
                    final var storage = f.storage();
                    final var stone = appeng.api.stacks.AEItemKey.of(Items.STONE);
                    helper.assertTrue(storage.insert(stone, 500, appeng.api.config.Actionable.MODULATE, f.source()) == 500, "Real ME storage rejected test stone");
                    f.bus.getConfig().setStack(0, new appeng.api.stacks.GenericStack(stone, 1));
                    final var pauses = new ArrayList<Double>();
                    final var context = context(pauses);
                    final int[] expected = {1, 8, 32, 64, 96};
                    for (int speed = 0; speed < expected.length; speed++) {
                        f.speed(speed);
                        final var result = component.invoke("exportIntoSlot", context, side.get3DDataValue());
                        helper.assertTrue(((Number) result[0]).intValue() == expected[speed] && count(f.inventory(), Items.STONE) == expected[speed]
                            && storage.getAvailableStacks().get(stone) + count(f.inventory(), Items.STONE) == 500, "Wrong actual export speed/count at " + speed + ": result=" + java.util.Arrays.toString(result)
                            + ", target=" + count(f.inventory(), Items.STONE) + ", ME=" + storage.getAvailableStacks().get(stone) + ", power=" + f.grid().getEnergyService().getStoredPower());
                        f.returnInventory();
                    }
                    final var onlySlot = component.invoke("exportIntoSlot", context, side.get3DDataValue(), 2);
                    helper.assertTrue(((Number) onlySlot[0]).intValue() == 64 && f.inventory().getItem(1).getCount() == 64
                        && f.inventory().getItem(0).isEmpty(), "Explicit slot did not constrain a96-item operation");
                    f.returnInventory();
                    helper.assertTrue(storage.extract(stone, 62, appeng.api.config.Actionable.MODULATE, f.source()) == 62, "Cannot prepare partial slot");
                    f.inventory().setItem(0, new ItemStack(Items.STONE, 62));
                    helper.assertTrue(((Number) component.invoke("exportIntoSlot", context, side.get3DDataValue(), 1)[0]).intValue() == 2
                        && f.inventory().getItem(0).getCount() == 64, "Partial slot exported wrong remainder");
                    f.returnInventory();
                    for (int slot = 0; slot < f.inventory().getContainerSize(); slot++) f.inventory().setItem(slot, new ItemStack(Items.DIRT, 64));
                    noMove(helper, component.invoke("exportIntoSlot", context, side.get3DDataValue()), "no items moved");
                    helper.assertTrue(storage.getAvailableStacks().get(stone) == 500, "Full destination removed ME items");
                    f.inventory().clearContent();
                    error(helper, component, "invalid slot", side.get3DDataValue(), 0);
                    error(helper, component, "invalid slot", side.get3DDataValue(), 28);
                    error(helper, component, "invalid side", 6);
                    noMove(helper, component.invoke("exportIntoSlot", context, side.getOpposite().get3DDataValue()), "no export bus");
                    helper.getLevel().setBlockAndUpdate(f.target, Blocks.AIR.defaultBlockState());
                    noMove(helper, component.invoke("exportIntoSlot", context, side.get3DDataValue()), "no inventory");
                    helper.getLevel().setBlockAndUpdate(f.target, Blocks.FURNACE.defaultBlockState());
                    final var ore = appeng.api.stacks.AEItemKey.of(Items.IRON_ORE);
                    helper.assertTrue(storage.insert(ore, 8, appeng.api.config.Actionable.MODULATE, f.source()) == 8, "Cannot store furnace input");
                    f.bus.getConfig().setStack(0, new appeng.api.stacks.GenericStack(ore, 1));
                    final var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) helper.getLevel().getBlockEntity(f.target);
                    final var furnaceResult = component.invoke("exportIntoSlot", context, side.get3DDataValue(), 1);
                    if (side == Direction.DOWN) helper.assertTrue(((Number) furnaceResult[0]).intValue() == 8 && furnace.getItem(0).getCount() == 8, "Top-sided furnace input rejected");
                    else noMove(helper, furnaceResult, "no items moved");
                    helper.assertTrue(storage.getAvailableStacks().get(ore) + count(furnace, Items.IRON_ORE) == 8, "Sided furnace transfer lost/duplicated ore");
                    final int successful = side == Direction.DOWN ? 8 : 7;
                    helper.assertTrue(pauses.size() == successful && pauses.stream().allMatch(value -> value == 0.25), "Export paused on failure or lost success delay");
                    helper.getLevel().setBlockAndUpdate(f.target, Blocks.CHEST.defaultBlockState());
                    f.bus.getConfig().setStack(0, new appeng.api.stacks.GenericStack(stone, 1));
                    f.grid().getEnergyService().extractAEPower(Double.MAX_VALUE, appeng.api.config.Actionable.MODULATE, appeng.api.config.PowerMultiplier.CONFIG);
                    noMove(helper, component.invoke("exportIntoSlot", context, side.get3DDataValue()), "no items moved");
                    helper.assertTrue(storage.getAvailableStacks().get(stone) == 500 && count(f.inventory(), Items.STONE) == 0, "Power deficit exported free items");
                  } catch (net.minecraft.gametest.framework.GameTestAssertException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                  finally { f.environment.node().remove(); }
                }).thenSucceed();
        }

        static void fuzzy(final GameTestHelper helper) {
            final var f = new Fixture(helper, Direction.SOUTH);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(f.bus.getGridNode() != null, "Waiting for fuzzy export node"))
                .thenWaitUntil(f::awaitPoweredGrid)
                .thenExecute(() -> {
                  try {
                    final var filter = new ItemStack(Items.IRON_PICKAXE); filter.setDamageValue(150);
                    final var match = new ItemStack(Items.IRON_PICKAXE); match.setDamageValue(170);
                    match.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Fuzzy Fixture"));
                    final var other = new ItemStack(Items.IRON_PICKAXE); other.setDamageValue(100);
                    final var matching = appeng.api.stacks.AEItemKey.of(match); final var excluded = appeng.api.stacks.AEItemKey.of(other);
                    helper.assertTrue(f.storage().insert(matching, 1, appeng.api.config.Actionable.MODULATE, f.source()) == 1
                        && f.storage().insert(excluded, 1, appeng.api.config.Actionable.MODULATE, f.source()) == 1, "Fuzzy real cell rejected tools");
                    f.bus.getConfig().setStack(0, new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(filter), 1));
                    final var component = (li.cil.oc.api.network.Component) f.environment.node();
                    noMove(helper, component.invoke("exportIntoSlot", null, 3), "no items moved");
                    helper.assertTrue(f.bus.getUpgrades().insertItem(0, new ItemStack(item("fuzzy_card")), false).isEmpty(), "Bus rejected fuzzy card");
                    f.bus.getConfigManager().putSetting(appeng.api.config.Settings.FUZZY_MODE, appeng.api.config.FuzzyMode.PERCENT_50);
                    helper.assertTrue(((Number) component.invoke("exportIntoSlot", null, 3, 2)[0]).intValue() == 1
                        && ItemStack.matches(f.inventory().getItem(1), match) && f.storage().getAvailableStacks().get(excluded) == 1,
                        "Fuzzy threshold or original item components were lost");
                    f.bus.getConfigManager().putSetting(appeng.api.config.Settings.FUZZY_MODE, appeng.api.config.FuzzyMode.IGNORE_ALL);
                    helper.assertTrue(((Number) component.invoke("exportIntoSlot", null, 3)[0]).intValue() == 1
                        && count(f.inventory(), Items.IRON_PICKAXE) == 2, "Ignore-all fuzzy filter did not transfer remaining tool");
                  } catch (net.minecraft.gametest.framework.GameTestAssertException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                  finally { f.environment.node().remove(); }
                }).thenSucceed();
        }

        static void nativeExport(final GameTestHelper helper) {
            final var f = new Fixture(helper, Direction.SOUTH);
            final var computerPos = new BlockPos(2, 4, 4); final var adapterPos = new BlockPos(3, 4, 4);
            helper.setBlock(adapterPos, li.cil.oc.common.ModBlocks.ADAPTER.get()); helper.setBlock(computerPos, li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final var cpu = new ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local bus = component.proxy(assert(component.list('me_exportbus')()))
                assert(bus.exportIntoSlot(3, 2) == 1)
                local ok, reason = pcall(bus.exportIntoSlot, 3, 0)
                assert(not ok and tostring(reason):find('invalid slot', 1, true))
                component.proxy(component.list('eeprom')()).setData('ae2-export-ok')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors(); li.cil.oc.api.Network.joinOrCreateNetwork(adapter); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(f.bus.getGridNode() != null, "Waiting for native export node"))
                .thenWaitUntil(f::awaitPoweredGrid)
                .thenExecute(() -> {
                    final var stone = appeng.api.stacks.AEItemKey.of(Items.STONE);
                    helper.assertTrue(f.storage().insert(stone, 12, appeng.api.config.Actionable.MODULATE, f.source()) == 12, "Cannot prepare native export storage");
                    f.bus.getConfig().setStack(0, new appeng.api.stacks.GenericStack(stone, 1));
                    helper.assertTrue(computer.machine().start(), "Native export computer did not start");
                }).thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(
                    li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("ae2-export-ok"), "Native export failed: " + computer.machine().lastError()))
                .thenExecute(() -> {
                    helper.assertTrue(f.inventory().getItem(1).is(Items.STONE) && f.inventory().getItem(1).getCount() == 1 && f.inventory().getItem(0).isEmpty()
                        && f.storage().getAvailableStacks().get(appeng.api.stacks.AEItemKey.of(Items.STONE)) == 11, "Native callback did not move one real item into requested slot");
                    f.environment.node().remove();
                }).thenSucceed();
        }

        static void commitRecovery(final GameTestHelper helper) {
            final var f = new Fixture(helper, Direction.SOUTH);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(f.bus.getGridNode() != null, "Waiting for recovery export node"))
                .thenWaitUntil(f::awaitPoweredGrid)
                .thenExecute(() -> {
                    try {
                        final var key = appeng.api.stacks.AEItemKey.of(Items.STONE);
                        final var storage = f.storage();
                        helper.assertTrue(storage.insert(key, 64, appeng.api.config.Actionable.MODULATE, f.source()) == 64, "Cannot seed recovery cell");
                        final var dropped = new ArrayList<ItemStack>();
                        final var partial = new ChangingTarget(3, () -> { });
                        helper.assertTrue(li.cil.oc.common.integration.ae2.ItemExport.transfer(f.grid().getEnergyService(), storage, key, f.source(), partial, 0, 8, dropped::add) == 3
                            && partial.getStackInSlot(0).getCount() == 3 && storage.getAvailableStacks().get(key) == 61 && dropped.isEmpty(), "Partial commit failed to refund actual rejected items");
                        final var rejected = new ChangingTarget(0, () -> { });
                        helper.assertTrue(li.cil.oc.common.integration.ae2.ItemExport.transfer(f.grid().getEnergyService(), storage, key, f.source(), rejected, 0, 8, dropped::add) == 0
                            && rejected.getStackInSlot(0).isEmpty() && storage.getAvailableStacks().get(key) == 61 && dropped.isEmpty(), "Rejected commit lost items");
                        final var limitedStorage = new appeng.api.storage.MEStorage() {
                            @Override public net.minecraft.network.chat.Component getDescription() { return storage.getDescription(); }
                            @Override public long extract(final appeng.api.stacks.AEKey what, final long amount, final appeng.api.config.Actionable mode, final appeng.api.networking.security.IActionSource source) {
                                return storage.extract(what, mode == appeng.api.config.Actionable.MODULATE ? Math.min(2, amount) : amount, mode, source);
                            }
                        };
                        final var limited = new ChangingTarget(8, () -> { });
                        helper.assertTrue(li.cil.oc.common.integration.ae2.ItemExport.transfer(f.grid().getEnergyService(), limitedStorage, key, f.source(), limited, 0, 8, dropped::add) == 2
                            && limited.getStackInSlot(0).getCount() == 2 && storage.getAvailableStacks().get(key) == 59, "Actual extraction shortage duplicated simulated items");
                        final long[] filled = {0};
                        final var fullDuringCommit = new ChangingTarget(0, () -> filled[0] = storage.insert(key, 1_000_000, appeng.api.config.Actionable.MODULATE, f.source()));
                        helper.assertTrue(li.cil.oc.common.integration.ae2.ItemExport.transfer(f.grid().getEnergyService(), storage, key, f.source(), fullDuringCommit, 0, 8, dropped::add) == 0,
                            "Rejected full-cell operation reported moved items");
                        helper.assertTrue(filled[0] > 0 && dropped.size() == 1 && dropped.get(0).is(Items.STONE) && dropped.get(0).getCount() == 8
                            && storage.getAvailableStacks().get(key) + partial.getStackInSlot(0).getCount() + limited.getStackInSlot(0).getCount() + dropped.get(0).getCount() == 64 + filled[0],
                            "Full refund destination lost or duplicated extracted items");
                    } finally { f.environment.node().remove(); }
                }).thenSucceed();
        }

        /** A truthful handler whose capacity changes between simulation and commit. */
        static final class ChangingTarget extends net.neoforged.neoforge.items.ItemStackHandler {
            private final int accepts;
            private final Runnable onCommit;
            ChangingTarget(final int accepts, final Runnable onCommit) { super(1); this.accepts = accepts; this.onCommit = onCommit; }
            @Override public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
                if (simulate) return super.insertItem(slot, stack, true);
                onCommit.run();
                final int attempt = Math.min(accepts, stack.getCount());
                if (attempt == 0) return stack;
                final var rejected = super.insertItem(slot, stack.copyWithCount(attempt), false);
                return stack.copyWithCount(stack.getCount() - attempt + rejected.getCount());
            }
        }

        static void unloaded(final GameTestHelper helper) {
            final var level = helper.getLevel(); final var edge = new BlockPos(1_600_015, 80, -1_595_192);
            final var chunk = level.getChunkAt(edge);
            helper.assertTrue(!level.hasChunkAt(edge.east()), "Export boundary fixture requires unloaded east chunk");
            try {
                chunk.setBlockState(edge, BuiltInRegistries.BLOCK.get(id("cable_bus")).defaultBlockState(), false);
                final var host = (appeng.api.parts.IPartHost) chunk.getBlockEntity(edge);
                helper.assertTrue(host.addPart((appeng.api.parts.IPartItem<?>) item("export_bus"), Direction.EAST, null) != null, "Cannot install boundary export bus");
                final var environment = li.cil.oc.api.Driver.driverFor(level, edge, Direction.WEST).createEnvironment(level, edge, Direction.WEST);
                try {
                    helper.assertTrue(!level.hasChunkAt(edge.east()), "Boundary setup loaded target before callback");
                    noMove(helper, ((li.cil.oc.api.network.Component) environment.node()).invoke("exportIntoSlot", null, 5), "no inventory");
                    helper.assertTrue(!level.hasChunkAt(edge.east()), "Manual export forced unloaded target chunk");
                } finally { environment.node().remove(); }
            } catch (net.minecraft.gametest.framework.GameTestAssertException exception) { throw exception; }
            catch (Exception exception) { helper.fail(exception.toString()); }
            finally { chunk.setBlockState(edge, Blocks.AIR.defaultBlockState(), false); }
            helper.succeed();
        }

        static final class Fixture {
            final GameTestHelper helper; final BlockPos host; final BlockPos drive; final BlockPos target;
            final appeng.parts.automation.ExportBusPart bus;
            final li.cil.oc.api.network.ManagedEnvironment environment;
            Fixture(final GameTestHelper helper, final Direction side) {
                this.helper = helper; host = helper.absolutePos(new BlockPos(4, 4, 4)); target = host.relative(side); drive = host.relative(side.getOpposite());
                // A drive's front cannot carry an ME connection. Keep it perpendicular to this fixture's chain.
                final var driveState = BuiltInRegistries.BLOCK.get(id("drive")).defaultBlockState();
                helper.getLevel().setBlockAndUpdate(drive, appeng.api.orientation.IOrientationStrategy.get(driveState).setFacing(driveState,
                    side.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.NORTH));
                put(helper, host.relative(side.getOpposite(), 2), "controller"); put(helper, host.relative(side.getOpposite(), 3), "energy_cell");
                appeng.api.parts.PartHelper.setPart(helper.getLevel(), host, null, null, (appeng.api.parts.IPartItem<?>) item("fluix_glass_cable"));
                bus = (appeng.parts.automation.ExportBusPart) appeng.api.parts.PartHelper.setPart(helper.getLevel(), host, side, null, (appeng.api.parts.IPartItem<?>) item("export_bus"));
                helper.assertTrue(bus != null, "Cannot place real export bus");
                helper.assertTrue(bus.getUpgrades().insertItem(4, new ItemStack(item("redstone_card")), false).isEmpty(), "Bus rejected redstone card");
                bus.getConfigManager().putSetting(appeng.api.config.Settings.REDSTONE_CONTROLLED, appeng.api.config.RedstoneMode.HIGH_SIGNAL);
                helper.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
                final var cells = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, drive, Direction.UP);
                helper.assertTrue(cells != null && cells.insertItem(0, new ItemStack(item("item_storage_cell_1k")), false).isEmpty(), "Drive rejected export test cell");
                environment = li.cil.oc.api.Driver.driverFor(helper.getLevel(), host, side.getOpposite()).createEnvironment(helper.getLevel(), host, side.getOpposite());
            }
            appeng.api.networking.IGrid grid() { return bus.getGridNode().getGrid(); }
            appeng.api.storage.MEStorage storage() { return grid().getStorageService().getInventory(); }
            appeng.api.networking.security.IActionSource source() { return appeng.api.networking.security.IActionSource.ofMachine(bus); }
            net.minecraft.world.Container inventory() { return (net.minecraft.world.Container) helper.getLevel().getBlockEntity(target); }
            boolean driveActive() { final var h = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, drive, null); return h != null && h.getGridNode(Direction.UP) != null && h.getGridNode(Direction.UP).isActive(); }
            void power() { grid().getEnergyService().injectPower(100000, appeng.api.config.Actionable.MODULATE); }
            void awaitPoweredGrid() {
                helper.assertTrue(grid().getEnergyService().getMaxStoredPower() >= 200000, "Waiting for actual energy cell to join export grid");
                power();
                helper.assertTrue(bus.getGridNode().isActive() && driveActive(), "Waiting for real powered export grid");
            }
            void speed(final int count) { for (int i = 0; i < 4; i++) bus.getUpgrades().setItemDirect(i, i < count ? new ItemStack(item("speed_card")) : ItemStack.EMPTY); }
            void returnInventory() {
                for (int slot = 0; slot < inventory().getContainerSize(); slot++) {
                    final var stack = inventory().getItem(slot);
                    if (stack.isEmpty()) continue;
                    helper.assertTrue(storage().insert(appeng.api.stacks.AEItemKey.of(stack), stack.getCount(), appeng.api.config.Actionable.MODULATE, source()) == stack.getCount(), "Cannot return exported test items");
                    inventory().setItem(slot, ItemStack.EMPTY);
                }
            }
        }

        static void put(final GameTestHelper helper, final BlockPos pos, final String block) { helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id(block)).defaultBlockState()); }
        static ResourceLocation id(final String name) { return ResourceLocation.fromNamespaceAndPath("ae2", name); }
        static net.minecraft.world.item.Item item(final String name) { return BuiltInRegistries.ITEM.get(id(name)); }
        static int count(final net.minecraft.world.Container inventory, final net.minecraft.world.item.Item item) { int count = 0; for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot).is(item)) count += inventory.getItem(slot).getCount(); return count; }
        static li.cil.oc.api.machine.Context context(final List<Double> pauses) { return (li.cil.oc.api.machine.Context) java.lang.reflect.Proxy.newProxyInstance(li.cil.oc.api.machine.Context.class.getClassLoader(), new Class<?>[]{li.cil.oc.api.machine.Context.class}, (proxy, method, args) -> { if (method.getName().equals("pause")) { pauses.add((Double) args[0]); return true; } return null; }); }
        static void noMove(final GameTestHelper helper, final Object[] result, final String reason) { helper.assertTrue(result[0] == null && result[1].equals(reason), "Expected " + reason + ": " + java.util.Arrays.toString(result)); }
        static void error(final GameTestHelper helper, final li.cil.oc.api.network.Component component, final String reason, final Object... args) throws Exception { try { component.invoke("exportIntoSlot", null, args); } catch (IllegalArgumentException exception) { helper.assertTrue(reason.equals(exception.getMessage()), "Wrong export error " + exception.getMessage()); return; } helper.fail("Expected " + reason); }
    }
}
