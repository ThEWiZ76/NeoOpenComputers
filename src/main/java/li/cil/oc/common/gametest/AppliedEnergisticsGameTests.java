package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
public final class AppliedEnergisticsGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> appliedEnergisticsTests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("ae2")) {
            if (Boolean.getBoolean("neoopencomputers.testAE2")) throw new IllegalStateException("Requested AE2 fixture is missing");
            return List.of();
        }
        return List.of(new TestFunction("ae2", "ae2_controller_storage_contract", "neoopencomputers:empty", 300, 0, true, Loaded::storageContract),
            new TestFunction("ae2", "ae2_native_adapter", "neoopencomputers:empty", 400, 0, true, Loaded::nativeAdapter),
            new TestFunction("ae2", "ae2_native_crafting", "neoopencomputers:empty", 1000, 0, true, Loaded::nativeCrafting),
            new TestFunction("ae2", "ae2_native_block_interface_crafting", "neoopencomputers:empty", 1000, 0, true, helper -> Loaded.nativeCrafting(helper, "interface")),
            new TestFunction("ae2", "ae2_native_panel_crafting", "neoopencomputers:empty", 1000, 0, true, helper -> Loaded.nativeCrafting(helper, "panel")));
    }

    private static final class Loaded {
        static void nativeCrafting(final GameTestHelper helper) {
            nativeCrafting(helper, "controller");
        }

        static void nativeCrafting(final GameTestHelper helper, final String hostKind) {
            helper.setBlock(new BlockPos(1, 1, 1), block(hostKind.equals("interface") ? "interface" : "controller"));
            helper.setBlock(new BlockPos(2, 1, 1), block("drive"));
            helper.setBlock(new BlockPos(1, 1, 0), block("creative_energy_cell"));
            helper.setBlock(new BlockPos(2, 1, 0), block("1k_crafting_storage"));
            helper.setBlock(new BlockPos(2, 1, 2), block("pattern_provider"));
            helper.setBlock(new BlockPos(3, 1, 2), block("molecular_assembler"));
            helper.setBlock(new BlockPos(2, 2, 2), net.minecraft.world.level.block.Blocks.CHEST);
            final var craftingCpu = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 0)));
            final var cpuData = craftingCpu.saveWithFullMetadata(helper.getLevel().registryAccess());
            cpuData.putString("customName", "fixture-cpu");
            craftingCpu.loadWithComponents(cpuData, helper.getLevel().registryAccess());
            final var provider = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2)));
            @SuppressWarnings("unchecked") final var recipe = (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>) (Object)
                helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("oak_planks")).orElseThrow();
            final var ingredients = new net.minecraft.world.item.ItemStack[9];
            java.util.Arrays.fill(ingredients, net.minecraft.world.item.ItemStack.EMPTY);
            ingredients[0] = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG);
            final var pattern = appeng.api.crafting.PatternDetailsHelper.encodeCraftingPattern(recipe, ingredients,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 4), false, false);
            final var data = provider.saveWithFullMetadata(helper.getLevel().registryAccess());
            final var entry = (net.minecraft.nbt.CompoundTag) pattern.save(helper.getLevel().registryAccess());
            entry.putInt("Slot", 0);
            final var patterns = new net.minecraft.nbt.ListTag();
            patterns.add(entry);
            final var processing = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                java.util.List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG), 1)),
                java.util.List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.GOLD_INGOT), 1)));
            final var processingEntry = (net.minecraft.nbt.CompoundTag) processing.save(helper.getLevel().registryAccess());
            processingEntry.putInt("Slot", 1); patterns.add(processingEntry);
            final var missing = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                java.util.List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.BIRCH_LOG), 1)),
                java.util.List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 1)));
            final var missingEntry = (net.minecraft.nbt.CompoundTag) missing.save(helper.getLevel().registryAccess());
            missingEntry.putInt("Slot", 2); patterns.add(missingEntry);
            data.put("patterns", patterns);
            provider.loadWithComponents(data, helper.getLevel().registryAccess());
            final var drivePos = helper.absolutePos(new BlockPos(2, 1, 1));
            final var cells = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, drivePos, Direction.UP);
            helper.assertTrue(cells != null && cells.insertItem(0, new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ae2", "item_storage_cell_1k"))), false).isEmpty(), "Crafting fixture drive rejected cell");
            helper.setBlock(new BlockPos(1, 1, 2), li.cil.oc.common.ModBlocks.ADAPTER.get());
            helper.setBlock(new BlockPos(1, 1, 3), li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            if (hostKind.equals("panel")) {
                final var panelPos = helper.absolutePos(new BlockPos(3, 1, 1));
                appeng.api.parts.PartHelper.setPart(helper.getLevel(), panelPos, null, null,
                    (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "fluix_glass_cable")));
                helper.assertTrue(appeng.api.parts.PartHelper.setPart(helper.getLevel(), panelPos, Direction.NORTH, null,
                    (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", "cable_interface"))) != null, "Crafting fixture panel not installed");
                for (final var cable : List.of(new BlockPos(1, 2, 2), new BlockPos(1, 2, 1), new BlockPos(2, 2, 1))) {
                    helper.setBlock(cable, li.cil.oc.common.ModBlocks.CABLE.get());
                    li.cil.oc.api.Network.joinOrCreateNetwork(helper.getLevel().getBlockEntity(helper.absolutePos(cable)));
                }
                helper.setBlock(new BlockPos(3, 2, 1), li.cil.oc.common.ModBlocks.ADAPTER.get());
                final li.cil.oc.common.blockentity.AdapterBlockEntity panelAdapter = helper.getBlockEntity(new BlockPos(3, 2, 1));
                panelAdapter.onLoad(); panelAdapter.refreshNeighbors(); li.cil.oc.api.Network.joinOrCreateNetwork(panelAdapter);
            }
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(new BlockPos(1, 1, 2));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 3));
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4, cpu);
            computer.setItem(2, new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local controller = component.proxy(assert(component.list('me_controller')()))
                local recipes = controller.getCraftables({name='minecraft:oak_planks', size=4})
                assert(#recipes == 1, 'expected one real plank recipe')
                local recipe = recipes[1]
                local output = recipe.getItemStack()
                assert(output.name == 'minecraft:oak_planks' and output.size == 4, 'recipe output count lost')
                assert(type(recipe.requesting()) == 'number')
                local available = controller.getItemsInNetwork({name='minecraft:oak_planks'})
                assert(#available == 1 and available[1].size == 0 and available[1].isCraftable, 'craftable-only output missing')
                assert(#controller.getCraftables({name='minecraft:diamond'}) == 0)
                local ok = pcall(recipe.request, 0)
                assert(not ok, 'zero-size request accepted')
                local job = recipe.request(8, false, 'fixture-cpu')
                local done, reason = job.isDone()
                assert(done == nil and reason == 'computing', 'new request did not report computing')
                repeat
                  computer.pullSignal(0.05)
                  done, reason = job.isDone()
                  assert(not reason or reason == 'computing', tostring(reason))
                until done
                assert(job.isCanceled() == false)
                local canceled, why = job.cancel()
                assert(canceled == false and why == 'job already completed')
                local stored = controller.getItemsInNetwork({name='minecraft:oak_planks'})
                assert(#stored == 1 and stored[1].size == 8, 'completed output not in real storage')
                local unavailable = recipe.request(4, true, 'missing-fixture-cpu')
                repeat computer.pullSignal(0.05); done, reason = unavailable.isDone() until done ~= nil
                assert(done == false and tostring(reason):find('request failed', 1, true))
                local missing = controller.getCraftables({name='minecraft:cobblestone'})[1].request()
                repeat computer.pullSignal(0.05); done, reason = missing.isDone() until done ~= nil
                assert(done == false and tostring(reason):find('incomplete_plan', 1, true), 'missing ingredients did not fail')
                local pending = controller.getCraftables({name='minecraft:gold_ingot'})[1].request()
                local computing
                repeat computer.pullSignal(0.05); computing, reason = pending.isDone() until computing ~= nil
                assert(computing == false and not reason, 'processing cancel request failed')
                local busy = recipe.request(4, true, 'fixture-cpu')
                repeat computer.pullSignal(0.05); done, reason = busy.isDone() until done ~= nil
                assert(done == false and tostring(reason):find('cpu_busy', 1, true), 'busy CPU request did not fail')
                assert(pending.cancel() == true and pending.isCanceled() == true)
                component.proxy(component.list('eeprom')()).setData('ae2-crafting-ok')
                while true do computer.pullSignal() end
                """.replace("component.list('me_controller')", "component.list('" + (hostKind.equals("controller") ? "me_controller" : "me_interface") + "')")));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors();
            li.cil.oc.api.Network.joinOrCreateNetwork(adapter); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            final appeng.api.networking.IGrid[] grid = {null};
            final li.cil.oc.api.machine.Value[] aliases = new li.cil.oc.api.machine.Value[3];
            final li.cil.oc.api.machine.Value[] failureRequests = new li.cil.oc.api.machine.Value[2];
            final li.cil.oc.api.machine.Value[] failureRecipe = new li.cil.oc.api.machine.Value[1];
            helper.startSequence().thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, drivePos, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.UP) != null && host.getGridNode(Direction.UP).isActive(), "Waiting for crafting AE2 grid");
                grid[0] = host.getGridNode(Direction.UP).getGrid();
                helper.assertTrue(grid[0].getCraftingService().isCraftable(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_PLANKS)), "Real pattern provider has not mounted recipe");
                helper.assertTrue(grid[0].getCraftingService().getCpus().size() == 1, "Waiting for real crafting CPU");
            }).thenExecute(() -> {
              try {
                final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
                final var environment = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH).createEnvironment(helper.getLevel(), pos, Direction.NORTH);
                final var database = new li.cil.oc.common.component.DatabaseEnvironment(1);
                try {
                    li.cil.oc.api.Network.joinNewNetwork(environment.node()); environment.node().connect(database.node());
                    ((li.cil.oc.api.network.Component) environment.node()).invoke("store", null, java.util.Map.of("name", "minecraft:oak_planks"));
                    helper.assertTrue(database.getStackInSlot(0).is(net.minecraft.world.item.Items.OAK_PLANKS) && database.getStackInSlot(0).getCount() == 4,
                        "Craftable-only catalogue entry lost recipe output count");
                    helper.assertTrue(database.getPhysicalStackInSlot(0).isEmpty(), "Craftable catalogue created physical output");
                } finally { environment.node().remove(); database.node().remove(); }
              } catch (RuntimeException exception) { throw exception; }
              catch (Exception exception) { helper.fail(exception.toString()); }
                helper.assertTrue(grid[0].getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG), 16,
                    appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 16, "Real crafting storage rejected logs");
                helper.assertTrue(computer.machine().start(), "Native crafting computer did not start");
            }).thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6))
                .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("ae2-crafting-ok"),
                "Native AE2 crafting program failed: " + computer.machine().lastError()))
                .thenIdle(60).thenExecute(() -> {
                  try {
                    final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
                    final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH);
                    final var environment = driver.createEnvironment(helper.getLevel(), pos, Direction.NORTH);
                    try {
                        final var component = (li.cil.oc.api.network.Component) environment.node();
                        final var craftable = (li.cil.oc.api.machine.Value) ((Object[]) component.invoke("getCraftables", null,
                            java.util.Map.of("name", "minecraft:gold_ingot"))[0])[0];
                        final var recipeTag = new net.minecraft.nbt.CompoundTag(); craftable.save(recipeTag);
                        final var restoredRecipe = new li.cil.oc.common.integration.ae2.CraftableValue(); restoredRecipe.load(recipeTag);
                        aliases[0] = (li.cil.oc.api.machine.Value) computer.machine().invoke(restoredRecipe, "request", new Object[0])[0];
                        final var requestTag = new net.minecraft.nbt.CompoundTag(); aliases[0].save(requestTag);
                        for (int index = 1; index < aliases.length; index++) {
                            aliases[index] = new li.cil.oc.common.integration.ae2.CraftingStatusValue(); aliases[index].load(requestTag);
                        }
                        for (final var alias : aliases) {
                            final var state = computer.machine().invoke(alias, "isDone", new Object[0]);
                            helper.assertTrue(state[0] == null && state[1].equals("computing"), "New/restored request lost computing state");
                        }
                    } finally { environment.node().remove(); }
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenWaitUntil(() -> {
                  try {
                    for (final var alias : aliases) {
                        final var state = computer.machine().invoke(alias, "isDone", new Object[0]);
                        helper.assertTrue(Boolean.FALSE.equals(state[0]) && state.length == 1, "Restored aliases did not share one submitted job");
                    }
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenExecute(() -> {
                  try {
                    helper.assertTrue(Boolean.TRUE.equals(computer.machine().invoke(aliases[1], "cancel", new Object[0])[0]), "Restored alias could not cancel request");
                    for (final var alias : aliases) helper.assertTrue(Boolean.TRUE.equals(computer.machine().invoke(alias, "isCanceled", new Object[0])[0]), "Cancel did not propagate between restored aliases");
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenIdle(60).thenExecute(() -> {
                    final var items = grid[0].getStorageService().getInventory().getAvailableStacks();
                    final net.minecraft.world.level.block.entity.ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(2, 2, 2));
                    long externalLogs = 0;
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(net.minecraft.world.item.Items.OAK_LOG)) externalLogs += chest.getItem(slot).getCount();
                    helper.assertTrue((items.get(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG)) + externalLogs) * 4
                        + items.get(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_PLANKS)) == 64,
                        "Complete/cancel lost or duplicated real crafting ingredients/output");
                }).thenExecute(() -> {
                  try {
                    helper.assertTrue(grid[0].getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG), 4000,
                        appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 4000, "Small-CPU fixture could not store actual ingredients");
                    final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
                    final var environment = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH).createEnvironment(helper.getLevel(), pos, Direction.NORTH);
                    try {
                        failureRecipe[0] = (li.cil.oc.api.machine.Value) ((Object[]) ((li.cil.oc.api.network.Component) environment.node()).invoke("getCraftables", null,
                            java.util.Map.of("name", "minecraft:oak_planks"))[0])[0];
                    } finally { environment.node().remove(); }
                    failureRequests[0] = (li.cil.oc.api.machine.Value) computer.machine().invoke(failureRecipe[0], "request", new Object[]{16000, true, "fixture-cpu"})[0];
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenWaitUntil(() -> {
                  try {
                    final var state = computer.machine().invoke(failureRequests[0], "isDone", new Object[0]);
                    helper.assertTrue(Boolean.FALSE.equals(state[0]) && state.length == 2 && state[1].toString().contains("cpu_too_small"), "Oversized plan did not report cpu_too_small: " + java.util.Arrays.toString(state));
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenExecute(() -> helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(2, 1, 0)), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> helper.assertTrue(grid[0].getCraftingService().getCpus().isEmpty(), "Removed CPU still registered"))
                .thenExecute(() -> {
                  try { failureRequests[1] = (li.cil.oc.api.machine.Value) computer.machine().invoke(failureRecipe[0], "request", new Object[]{4})[0]; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenWaitUntil(() -> {
                  try {
                    final var state = computer.machine().invoke(failureRequests[1], "isDone", new Object[0]);
                    helper.assertTrue(Boolean.FALSE.equals(state[0]) && state.length == 2 && state[1].toString().contains("no_cpu_found"), "Missing CPU did not report no_cpu_found: " + java.util.Arrays.toString(state));
                  } catch (RuntimeException exception) { throw exception; }
                  catch (Exception exception) { helper.fail(exception.toString()); }
                }).thenExecute(() -> {
                    final var items = grid[0].getStorageService().getInventory().getAvailableStacks();
                    final net.minecraft.world.level.block.entity.ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(2, 2, 2));
                    long externalLogs = 0;
                    for (int slot = 0; slot < chest.getContainerSize(); slot++) if (chest.getItem(slot).is(net.minecraft.world.item.Items.OAK_LOG)) externalLogs += chest.getItem(slot).getCount();
                    helper.assertTrue((items.get(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG)) + externalLogs) * 4
                        + items.get(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.OAK_PLANKS)) == 16064, "Rejected CPU requests changed real ingredients/output");
                }).thenSucceed();
        }

        static void storageContract(final GameTestHelper helper) {
            helper.setBlock(new BlockPos(1, 1, 1), block("controller"));
            helper.setBlock(new BlockPos(2, 1, 1), block("drive"));
            helper.setBlock(new BlockPos(1, 1, 0), block("energy_cell"));
            helper.setBlock(new BlockPos(2, 1, 0), block("1k_crafting_storage"));
            final var drivePos = helper.absolutePos(new BlockPos(2, 1, 1));
            final var cells = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, drivePos, Direction.UP);
            helper.assertTrue(cells != null, "Real AE2 drive lacks item capability");
            for (int slot = 0; slot < 2; slot++) {
                final var cell = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", slot == 0 ? "item_storage_cell_1k" : "fluid_storage_cell_1k"));
                helper.assertTrue(cells.insertItem(slot, new net.minecraft.world.item.ItemStack(cell), false).isEmpty(), "Drive rejected real storage cell");
            }
            final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH);
            helper.assertTrue(driver != null, "No OC driver for real AE2 controller");
            final var environment = driver.createEnvironment(helper.getLevel(), pos, Direction.NORTH);
            helper.startSequence().thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.NORTH) != null, "Waiting for AE2 controller node");
            }).thenExecute(() -> helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null)
                .getGridNode(Direction.NORTH).getGrid().getEnergyService().injectPower(10000, appeng.api.config.Actionable.MODULATE))
            .thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, drivePos, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.UP) != null && host.getGridNode(Direction.UP).isActive(), "Waiting for powered/channelled AE2 drive");
            }).thenIdle(5).thenExecute(() -> {
              try {
                final var component = (li.cil.oc.api.network.Component) environment.node();
                helper.assertTrue(component.name().equals("me_controller"), "AE2 controller exposed generic component " + component.name());
                helper.assertTrue(component.methods().containsAll(List.of("getItemsInNetwork", "getFluidsInNetwork", "getCpus", "store",
                    "getAvgPowerInjection", "getAvgPowerUsage", "getIdlePowerUsage", "getMaxStoredPower", "getStoredPower", "isNetworkPowered", "getEnergyDemand")),
                    "AE2 controller lacks network callbacks");
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.NORTH) != null, "AE2 controller has no grid node");
                final var grid = host.getGridNode(Direction.NORTH).getGrid();
                grid.getEnergyService().injectPower(10000, appeng.api.config.Actionable.MODULATE);
                final var storage = grid.getStorageService().getInventory();
                final var stone = appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.STONE);
                final var diamond = appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.DIAMOND);
                final var water = appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);
                final var source = appeng.api.networking.security.IActionSource.empty();
                helper.assertTrue(storage.insert(stone, 123, appeng.api.config.Actionable.MODULATE, source) == 123, "Real grid rejected stone");
                helper.assertTrue(storage.insert(diamond, 7, appeng.api.config.Actionable.MODULATE, source) == 7, "Real grid rejected diamond");
                helper.assertTrue(storage.insert(water, 1500, appeng.api.config.Actionable.MODULATE, source) == 1500, "Real grid rejected water");
                final var items = (Object[]) component.invoke("getItemsInNetwork", null)[0];
                helper.assertTrue(items.length == 2, "Item snapshot must exclude fluid keys");
                final var filtered = (Object[]) component.invoke("getItemsInNetwork", null, java.util.Map.of("name", "minecraft:stone", "size", 123.0))[0];
                helper.assertTrue(filtered.length == 1 && ((java.util.Map<?, ?>) filtered[0]).get("isCraftable").equals(false), "Item filter/quantity/craftability mismatch");
                helper.assertTrue(((Object[]) component.invoke("getItemsInNetwork", null, java.util.Map.of("size", 123.5))[0]).length == 0,
                    "Fractional filter quantity incorrectly truncated");
                final var fluids = (Object[]) component.invoke("getFluidsInNetwork", null)[0];
                helper.assertTrue(fluids.length == 1 && ((java.util.Map<?, ?>) fluids[0]).get("name").equals("minecraft:water")
                    && ((Number) ((java.util.Map<?, ?>) fluids[0]).get("amount")).longValue() == 1500, "Fluid amount is not millibuckets");
                for (final var entry : java.util.Map.of("getStoredPower", grid.getEnergyService().getStoredPower(),
                    "getMaxStoredPower", grid.getEnergyService().getMaxStoredPower(), "getIdlePowerUsage", grid.getEnergyService().getIdlePowerUsage(),
                    "getAvgPowerUsage", grid.getEnergyService().getAvgPowerUsage(), "getAvgPowerInjection", grid.getEnergyService().getAvgPowerInjection(),
                    "getEnergyDemand", grid.getEnergyService().getEnergyDemand(Double.MAX_VALUE)).entrySet()) {
                    helper.assertTrue(((Number) component.invoke(entry.getKey(), null)[0]).doubleValue() == entry.getValue(), "Energy callback mismatch: " + entry.getKey());
                }
                helper.assertTrue(component.invoke("isNetworkPowered", null)[0].equals(grid.getEnergyService().isNetworkPowered()), "Powered state mismatch");
                final var cpus = (Object[]) component.invoke("getCpus", null)[0];
                helper.assertTrue(cpus.length == 1 && ((Number) ((java.util.Map<?, ?>) cpus[0]).get("storage")).longValue() == 1024, "Real crafting CPU descriptor missing");
                final double[] budget = {0};
                final var context = (li.cil.oc.api.machine.Context) java.lang.reflect.Proxy.newProxyInstance(
                    li.cil.oc.api.machine.Context.class.getClassLoader(), new Class<?>[]{li.cil.oc.api.machine.Context.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("consumeCallBudget")) budget[0] += (double) arguments[0];
                        return null;
                    });
                component.invoke("getEnergyDemand", context);
                helper.assertTrue(budget[0] == 1.5, "Energy demand callback lost upstream call budget");
                expectError(helper, component, "store", "no database upgrade found");
                final var database = new li.cil.oc.common.component.DatabaseEnvironment(3);
                try {
                    li.cil.oc.api.Network.joinNewNetwork(environment.node());
                    environment.node().connect(database.node());
                    database.setPhysicalStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
                    component.invoke("store", null);
                    helper.assertTrue(database.getStackInSlot(0).is(net.minecraft.world.item.Items.DIRT), "Store overwrote occupied database entry");
                    helper.assertTrue(!database.getStackInSlot(1).isEmpty() && !database.getStackInSlot(2).isEmpty()
                        && !net.minecraft.world.item.ItemStack.isSameItemSameComponents(database.getStackInSlot(1), database.getStackInSlot(2)), "Store skipped or duplicated item descriptions");
                    helper.assertTrue(database.getPhysicalStackInSlot(1).isEmpty() && database.getPhysicalStackInSlot(2).isEmpty(), "Store created physical items");
                    component.invoke("store", null, null, database.node().address(), 3, 100);
                    expectError(helper, component, "store", "invalid slot", null, database.node().address(), 0);
                    expectError(helper, component, "store", "invalid slot", null, database.node().address(), 4);
                    expectError(helper, component, "store", "no such component", null, "not-an-address");
                    expectError(helper, component, "store", "not a database", null, environment.node().address());
                    database.setStackInSlot(2, net.minecraft.world.item.ItemStack.EMPTY);
                    component.invoke("store", null, java.util.Map.of("name", "minecraft:stone"), database.node().address(), 3, 0);
                    helper.assertTrue(database.getStackInSlot(2).isEmpty(), "Zero-count store wrote an entry");
                    component.invoke("store", null, java.util.Map.of("name", "minecraft:stone"), database.node().address(), 3, 1);
                    helper.assertTrue(database.getStackInSlot(2).is(net.minecraft.world.item.Items.STONE), "Filtered addressed store wrote wrong entry");
                    helper.assertTrue(storage.getAvailableStacks().get(stone) == 123 && storage.getAvailableStacks().get(diamond) == 7,
                        "Read/store callbacks changed real grid contents");
                } finally { database.node().remove(); }
                helper.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                expectError(helper, component, "getItemsInNetwork", "no controller");
              } catch (RuntimeException exception) { throw exception; }
              catch (Exception exception) { helper.fail(exception.toString()); }
              finally { environment.node().remove(); }
            }).thenSucceed();
        }

        static net.minecraft.world.level.block.Block block(final String name) {
            final var id = ResourceLocation.fromNamespaceAndPath("ae2", name);
            final var block = BuiltInRegistries.BLOCK.get(id);
            if (block == net.minecraft.world.level.block.Blocks.AIR) throw new IllegalStateException("Missing AE2 fixture block " + id);
            return block;
        }

        static void expectError(final GameTestHelper helper, final li.cil.oc.api.network.Component component,
                                final String method, final String message, final Object... arguments) throws Exception {
            try { component.invoke(method, null, arguments); }
            catch (IllegalArgumentException | IllegalStateException exception) {
                helper.assertTrue(message.equals(exception.getMessage()), "Wrong " + method + " error: " + exception.getMessage());
                return;
            }
            helper.fail(method + " did not reject " + message);
        }

        static void nativeAdapter(final GameTestHelper helper) {
            helper.setBlock(new BlockPos(1, 1, 1), block("controller"));
            helper.setBlock(new BlockPos(2, 1, 1), block("drive"));
            helper.setBlock(new BlockPos(2, 1, 2), block("creative_energy_cell"));
            final var drivePos = helper.absolutePos(new BlockPos(2, 1, 1));
            final var cells = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, drivePos, Direction.UP);
            helper.assertTrue(cells != null && cells.insertItem(0, new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ae2", "item_storage_cell_1k"))), false).isEmpty(), "Native fixture drive rejected cell");
            helper.setBlock(new BlockPos(1, 1, 2), li.cil.oc.common.ModBlocks.ADAPTER.get());
            helper.setBlock(new BlockPos(1, 1, 3), li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(new BlockPos(1, 1, 2));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 3));
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4, cpu);
            computer.setItem(2, new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local address = component.list('me_controller')()
                assert(address, 'adapter did not expose me_controller')
                local controller = component.proxy(address)
                local items = controller.getItemsInNetwork({name='minecraft:stone', size=123})
                assert(#items == 1 and items[1].name == 'minecraft:stone' and items[1].size == 123 and items[1].isCraftable == false)
                assert(#controller.getItemsInNetwork({size=123.5}) == 0)
                assert(type(controller.getFluidsInNetwork()) == 'table')
                assert(type(controller.getCpus()) == 'table')
                assert(type(controller.getStoredPower()) == 'number')
                assert(type(controller.isNetworkPowered()) == 'boolean')
                component.proxy(component.list('eeprom')()).setData('ae2-ok')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors();
            li.cil.oc.api.Network.joinOrCreateNetwork(adapter);
            li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(1000);
            helper.startSequence().thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, drivePos, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.UP) != null && host.getGridNode(Direction.UP).isActive(), "Waiting for native AE2 drive");
            }).thenExecute(() -> {
                final var grid = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, drivePos, null).getGridNode(Direction.UP).getGrid();
                helper.assertTrue(grid.getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.STONE), 123,
                    appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 123, "Native fixture storage rejected stone");
                helper.assertTrue(computer.machine().start(), "Native AE2 computer did not start");
            }).thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6))
                .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("ae2-ok"),
                "Native AE2 adapter program failed: " + computer.machine().lastError())).thenSucceed();
        }
    }
}
