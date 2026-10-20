package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

/** Opt-in real process restart, isolated from the ordinary GameTest structures. */
@GameTestHolder(NeoOpenComputers.MODID)
public final class AppliedEnergisticsProcessRestartGameTests {

    @GameTestGenerator
    public static Collection<TestFunction> restartTests() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty() || !net.neoforged.fml.ModList.get().isLoaded("ae2")) return List.of();
        return Loaded.tests(phase);
    }

    private static final class Loaded {
        static Collection<TestFunction> tests(final String phase) {
            final var fixtures = List.of(new Fixture(new BlockPos(328, 80, 328), false, false),
                new Fixture(new BlockPos(360, 80, 328), true, false), new Fixture(new BlockPos(392, 80, 328), true, true));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
                final var level = event.getServer().overworld();
                for (final var fixture : fixtures) {
                    if (level.hasChunkAt(fixture.COMPUTER) && level.getBlockEntity(fixture.COMPUTER) instanceof li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer) {
                        ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
                    }
                }
            });
            return fixtures.stream().map(fixture -> new TestFunction("ae2Restart", "ae2_process_restart_" + (fixture.delayedCompletion ? "delayed_" : fixture.offline ? "offline_" : "active_") + phase,
                "neoopencomputers:empty", 1000, 0, true, helper -> fixture.run(helper, phase.equals("prepare")))).toList();
        }
    }

    private static final class Fixture {
        private final BlockPos ORIGIN;
        private final BlockPos COMPUTER;
        private final BlockPos RECORD;
        private final boolean offline;
        private final boolean delayedCompletion;

        Fixture(final BlockPos origin, final boolean offline, final boolean delayedCompletion) {
            ORIGIN = origin; COMPUTER = origin.offset(1, 1, 3); RECORD = origin.offset(4, 1, 3); this.offline = offline;
            this.delayedCompletion = delayedCompletion;
        }

        void run(final GameTestHelper helper, final boolean prepare) {
            final var level = helper.getLevel();
            level.setChunkForced(ORIGIN.getX() >> 4, ORIGIN.getZ() >> 4, true);
            if (prepare) {
                helper.assertTrue(level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "AE2 checkpoint already exists; use a fresh restart world");
                place(helper, 1, 1, 1, "controller");
                place(helper, 2, 1, 1, "drive");
                place(helper, 1, 1, 0, "creative_energy_cell");
                place(helper, 2, 1, 0, "1k_crafting_storage");
                place(helper, 2, 1, 2, "pattern_provider");
                place(helper, 3, 1, 2, "molecular_assembler");
                level.setBlockAndUpdate(ORIGIN.offset(2, 0, 2), Blocks.FURNACE.defaultBlockState());
                final var cpu = level.getBlockEntity(ORIGIN.offset(2, 1, 0));
                final var data = cpu.saveWithFullMetadata(level.registryAccess());
                data.putString("customName", "restart-cpu"); cpu.loadWithComponents(data, level.registryAccess());
                installPatterns(helper);
                final var cells = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ORIGIN.offset(2, 1, 1), Direction.UP);
                helper.assertTrue(cells != null && cells.insertItem(0, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("ae2", "item_storage_cell_1k"))), false).isEmpty(), "Restart drive rejected cell");
                final var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) level.getBlockEntity(ORIGIN.offset(2, 0, 2));
                furnace.setItem(1, new ItemStack(Items.COAL, 2));
                level.setBlockAndUpdate(ORIGIN.offset(1, 1, 2), li.cil.oc.common.ModBlocks.ADAPTER.get().defaultBlockState());
                level.setBlockAndUpdate(COMPUTER, li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
                level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
            }
            final var record = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(RECORD);
            if (!prepare && delayedCompletion) { verifyDelayed(helper, record); return; }
            if (!prepare && offline) {
                helper.assertTrue(level.isEmptyBlock(COMPUTER), "Offline fixture computer was not removed before shutdown");
                final var saved = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag();
                helper.assertTrue(saved.getBoolean("completedWhileOffline") && saved.contains("computer"), "Offline snapshot/completion record missing");
                restoreComputer(helper, record);
            }
            helper.assertTrue(level.getBlockEntity(COMPUTER) instanceof li.cil.oc.common.blockentity.ComputerCaseBlockEntity, "AE2 restart computer missing");
            final var computer = (li.cil.oc.common.blockentity.ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
            final var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) level.getBlockEntity(ORIGIN.offset(2, 0, 2));
            if (prepare) {
                final var cpu = new ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
                ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
                computer.setItem(4, cpu);
                computer.setItem(2, new ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER2.get()));
                computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                    local eeprom = component.proxy(component.list('eeprom')())
                    assert(eeprom.getData() == '', 'AE2 restart program rebooted')
                    local retained = 731
                    local controller = component.proxy(assert(component.list('me_controller')()))
                    local planks = controller.getCraftables({name='minecraft:oak_planks'})[1]
                    local completed = planks.request(4, true, 'restart-cpu')
                    local done, reason
                    repeat computer.pullSignal(0.05); done, reason = completed.isDone(); assert(not reason or reason == 'computing', tostring(reason)) until done
                    local failed = planks.request(4, true, 'missing-restart-cpu')
                    repeat computer.pullSignal(0.05); done, reason = failed.isDone() until done ~= nil
                    assert(done == false and tostring(reason):find('no cpu named', 1, true))
                    local recipe = controller.getCraftables({name='minecraft:charcoal'})[1]
                    local job = recipe.request(1, false, 'restart-cpu')
                    local alias = job
                    repeat computer.pullSignal(0.05); done, reason = job.isDone() until done ~= nil
                    assert(done == false and not reason, tostring(reason))
                    eeprom.setData('prepared')
                    repeat until computer.pullSignal() == 'ae2_after_process_restart'
                    assert(retained == 731 and alias == job, 'AE2 Lua continuation/alias lost')
                    assert(recipe.getItemStack().name == 'minecraft:charcoal' and recipe.getItemStack().size == 1)
                    assert(type(recipe.requesting()) == 'number')
                    assert(completed.isDone() == true and completed.isCanceled() == false, 'completed status lost')
                    local accepted, why = completed.cancel()
                    assert(accepted == false and why == 'job already completed')
                    done, reason = failed.isDone()
                    assert(done == false and tostring(reason):find('no cpu named', 1, true), 'failed status lost')
                    assert(job.isCanceled() == false, 'pending job canceled at restart')
                    eeprom.setData('rebound')
                    repeat computer.pullSignal(0.05); done, reason = job.isDone(); assert(not reason or reason == 'computing', tostring(reason)) until done
                    assert(alias.isDone() == true and job.isCanceled() == false)
                    eeprom.setData('verified')
                    while true do computer.pullSignal() end
                    """));
                computer.onLoad();
                final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) level.getBlockEntity(ORIGIN.offset(1, 1, 2));
                adapter.onLoad(); adapter.refreshNeighbors();
                li.cil.oc.api.Network.joinOrCreateNetwork(adapter); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            } else {
                final var saved = record.getItem(0).get(DataComponents.CUSTOM_DATA);
                helper.assertTrue(saved != null && saved.copyTag().getString("phase").equals("prepared")
                    && saved.copyTag().getLong("pid") != ProcessHandle.current().pid(), "AE2 checkpoint was not prepared by another JVM");
                helper.assertTrue(computer.machine().isRunning(), "AE2 computer did not resume: " + computer.machine().lastError());
                computer.machine().signal("ae2_after_process_restart");
            }
            final var sequence = helper.startSequence().thenWaitUntil(() -> {
                final var grid = grid(helper);
                helper.assertTrue(grid.getCraftingService().isCraftable(appeng.api.stacks.AEItemKey.of(Items.CHARCOAL)), "AE2 restart pattern not mounted");
                helper.assertTrue(grid.getCraftingService().getCpus().size() == 1, "AE2 restart CPU not assembled");
            });
            if (prepare) {
                sequence.thenExecute(() -> {
                    helper.assertTrue(grid(helper).getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 8,
                        appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 8, "Restart storage rejected logs");
                    ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
                    helper.assertTrue(computer.machine().start(), "AE2 restart computer did not start");
                }).thenWaitUntil(() -> {
                    helper.assertTrue(marker(computer).equals("prepared"), "AE2 prepare not ready: " + computer.machine().lastError());
                    helper.assertTrue(furnace.getItem(0).is(Items.OAK_LOG) || furnace.getItem(2).is(Items.CHARCOAL), "Processing input not delivered to real furnace");
                }).thenExecute(() -> {
                    final var data = new CompoundTag();
                    data.putLong("pid", ProcessHandle.current().pid()); data.putString("phase", "prepared");
                    final var job = level.getBlockEntity(ORIGIN.offset(2, 1, 0)).saveWithFullMetadata(level.registryAccess()).getCompound("job").getCompound("link");
                    helper.assertTrue(job.hasUUID("craftId"), "Prepared CPU has no persisted crafting link");
                    data.putUUID("craftId", job.getUUID("craftId"));
                    data.putBoolean("pendingWhileOffline", delayedCompletion);
                    if (offline) {
                        data.put("computer", computer.saveWithFullMetadata(level.registryAccess()));
                        computer.onChunkUnloaded();
                        // This models BE unload, not the chunk-ticket lifecycle: keep the AE2 network loaded.
                        // Empty the detached fixture inventory before removing its block so no test hardware drops.
                        computer.clearContent();
                        level.setBlockAndUpdate(COMPUTER, Blocks.AIR.defaultBlockState());
                        helper.assertTrue(!computer.machine().isRunning() && level.isEmptyBlock(COMPUTER), "Old native computer is still active");
                    }
                    final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper);
                    NeoOpenComputers.LOGGER.info("AE2 restart {} snapshot in PID {} with CPU link {}", offline ? "offline" : "active", ProcessHandle.current().pid(), data.getUUID("craftId"));
                });
                if (offline && !delayedCompletion) {
                    sequence.thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.CHARCOAL), "Offline furnace output not ready"))
                        .thenExecute(() -> deliverOutput(helper, furnace))
                        .thenWaitUntil(() -> helper.assertTrue(grid(helper).getCraftingService().getCpus().stream().noneMatch(appeng.api.networking.crafting.ICraftingCPU::isBusy), "Offline CPU did not finish"))
                        .thenExecute(() -> {
                            assertItems(helper, furnace);
                            final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag(); data.putBoolean("completedWhileOffline", true);
                            record.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setChanged();
                            NeoOpenComputers.LOGGER.info("AE2 request completed with no OC computer loaded in PID {}; saved VM snapshot remains pre-completion", ProcessHandle.current().pid());
                        });
                }
                sequence.thenSucceed();
            } else {
                if (!offline) sequence.thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("rebound"), "AE2 persisted values not resumed: " + computer.machine().lastError()))
                    .thenIdle(2).thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.CHARCOAL), "Real furnace output not ready"))
                    .thenExecute(() -> {
                        final var expected = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag().getUUID("craftId");
                        final var job = level.getBlockEntity(ORIGIN.offset(2, 1, 0)).saveWithFullMetadata(level.registryAccess()).getCompound("job").getCompound("link");
                        helper.assertTrue(job.hasUUID("craftId") && job.getUUID("craftId").equals(expected), "Restart replaced or lost original CPU job link");
                        deliverOutput(helper, furnace);
                    });
                sequence.thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("verified"), "AE2 " + (offline ? "offline-completed" : "active") + " job/link continuation failed: " + computer.machine().lastError()))
                    .thenExecute(() -> {
                        assertItems(helper, furnace);
                        final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag();
                        data.putString("phase", "verified"); data.putLong("verifiedPid", ProcessHandle.current().pid());
                        record.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setChanged();
                        NeoOpenComputers.LOGGER.info("AE2 restart {} verified in PID {}; original CPU link {} completed with exact items", offline ? "offline" : "active", ProcessHandle.current().pid(), data.getUUID("craftId"));
                    }).thenSucceed();
            }
        }

        void verifyDelayed(final GameTestHelper helper, final net.minecraft.world.level.block.entity.ChestBlockEntity record) {
            final var level = helper.getLevel();
            final var saved = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag();
            helper.assertTrue(saved.getBoolean("pendingWhileOffline") && saved.getString("phase").equals("prepared")
                && saved.getLong("pid") > 0 && saved.getLong("pid") != ProcessHandle.current().pid(), "Delayed request was not prepared by another JVM");
            final var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) level.getBlockEntity(ORIGIN.offset(2, 0, 2));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity[] computer = {null};
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(grid(helper).getCraftingService().getCpus().size() == 1, "Delayed CPU not assembled"))
                .thenIdle(100).thenExecute(() -> {
                    helper.assertTrue(level.isEmptyBlock(COMPUTER), "An OC host loaded before delayed request recovery");
                    final var link = level.getBlockEntity(ORIGIN.offset(2, 1, 0)).saveWithFullMetadata(level.registryAccess()).getCompound("job").getCompound("link");
                    helper.assertTrue(link.hasUUID("craftId") && link.getUUID("craftId").equals(saved.getUUID("craftId"))
                        && !link.getBoolean("canceled"), "World-owned requester did not preserve pending CPU link without a Lua heap");
                }).thenWaitUntil(() -> helper.assertTrue(furnace.getItem(2).is(Items.CHARCOAL), "Delayed real furnace output not ready"))
                .thenExecute(() -> deliverOutput(helper, furnace))
                .thenWaitUntil(() -> helper.assertTrue(grid(helper).getCraftingService().getCpus().stream().noneMatch(appeng.api.networking.crafting.ICraftingCPU::isBusy), "Delayed CPU did not finish without OC host"))
                .thenExecute(() -> {
                    assertItems(helper, furnace);
                    helper.assertTrue(level.isEmptyBlock(COMPUTER), "OC host appeared while request finished");
                    computer[0] = restoreComputer(helper, record);
                    helper.assertTrue(computer[0].machine().isRunning(), "Delayed native snapshot did not resume");
                    computer[0].machine().signal("ae2_after_process_restart");
                }).thenWaitUntil(() -> helper.assertTrue(marker(computer[0]).equals("verified"), "Delayed old snapshot did not see completed request: " + computer[0].machine().lastError()))
                .thenExecute(() -> {
                    assertItems(helper, furnace);
                    final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag();
                    data.putString("phase", "verified"); data.putLong("verifiedPid", ProcessHandle.current().pid());
                    record.getItem(0).set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setChanged();
                    NeoOpenComputers.LOGGER.info("AE2 delayed restart verified in PID {}; CPU link {} survived100ticks with no OC host, then completed before heap restore",
                        ProcessHandle.current().pid(), data.getUUID("craftId"));
                }).thenSucceed();
        }

        li.cil.oc.common.blockentity.ComputerCaseBlockEntity restoreComputer(final GameTestHelper helper, final net.minecraft.world.level.block.entity.ChestBlockEntity record) {
            final var level = helper.getLevel();
            level.setBlockAndUpdate(COMPUTER, li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            final var restored = (li.cil.oc.common.blockentity.ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
            restored.loadWithComponents(record.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag().getCompound("computer"), level.registryAccess());
            restored.onLoad();
            return restored;
        }

        void deliverOutput(final GameTestHelper helper, final net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace) {
            final var storage = grid(helper).getStorageService().getInventory();
            final var key = appeng.api.stacks.AEItemKey.of(Items.CHARCOAL);
            final var source = appeng.api.networking.security.IActionSource.empty();
            helper.assertTrue(storage.insert(key, 1, appeng.api.config.Actionable.SIMULATE, source) == 1, "Restart network cannot accept furnace output");
            helper.assertTrue(furnace.removeItem(2, 1).is(Items.CHARCOAL), "Physical furnace output disappeared");
            helper.assertTrue(storage.insert(key, 1, appeng.api.config.Actionable.MODULATE, source) == 1, "Restart network rejected actual furnace output");
        }

        void assertItems(final GameTestHelper helper, final net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace) {
            final var items = grid(helper).getStorageService().getInventory().getAvailableStacks();
            helper.assertTrue(items.get(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG)) == 6
                && items.get(appeng.api.stacks.AEItemKey.of(Items.OAK_PLANKS)) == 4
                && items.get(appeng.api.stacks.AEItemKey.of(Items.CHARCOAL)) == 1
                && furnace.getItem(0).isEmpty() && furnace.getItem(2).isEmpty(), "Restart lost/duplicated crafting ingredients or output");
        }

        void place(final GameTestHelper helper, final int x, final int y, final int z, final String block) {
            helper.getLevel().setBlockAndUpdate(ORIGIN.offset(x, y, z), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ae2", block)).defaultBlockState());
        }

        void installPatterns(final GameTestHelper helper) {
            final var level = helper.getLevel();
            @SuppressWarnings("unchecked") final var recipe = (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>) (Object)
                level.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("oak_planks")).orElseThrow();
            final var input = new ItemStack[9]; java.util.Arrays.fill(input, ItemStack.EMPTY); input[0] = new ItemStack(Items.OAK_LOG);
            final var crafting = appeng.api.crafting.PatternDetailsHelper.encodeCraftingPattern(recipe, input, new ItemStack(Items.OAK_PLANKS, 4), false, false);
            final var processing = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 1)),
                List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.CHARCOAL), 1)));
            final var patterns = new net.minecraft.nbt.ListTag();
            for (final var pattern : List.of(crafting, processing)) {
                final var entry = (CompoundTag) pattern.save(level.registryAccess()); entry.putInt("Slot", patterns.size()); patterns.add(entry);
            }
            final var provider = level.getBlockEntity(ORIGIN.offset(2, 1, 2));
            final var data = provider.saveWithFullMetadata(level.registryAccess()); data.put("patterns", patterns);
            provider.loadWithComponents(data, level.registryAccess());
        }

        appeng.api.networking.IGrid grid(final GameTestHelper helper) {
            final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, ORIGIN.offset(2, 1, 1), null);
            helper.assertTrue(host != null && host.getGridNode(Direction.UP) != null && host.getGridNode(Direction.UP).isActive(), "AE2 restart drive not active");
            return host.getGridNode(Direction.UP).getGrid();
        }

        static String marker(final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer) {
            return new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(
                li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
