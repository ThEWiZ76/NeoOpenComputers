package li.cil.oc.common.gametest;

import li.cil.oc.api.machine.Value;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AppliedEnergisticsFailureGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> failures() {
        return net.neoforged.fml.ModList.get().isLoaded("ae2") ? Loaded.tests() : List.of();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            final var tests = new ArrayList<TestFunction>();
            for (final String kind : List.of("controller", "interface", "panel")) {
                for (final String failure : List.of("cpu_removed", "cpu_replaced", "host_removed", "host_replaced", "submitted_host_removed")) {
                    tests.add(new TestFunction("ae2Failures", "ae2_failure_" + kind + "_" + failure, "neoopencomputers:ae2_export", 600, 0, true,
                        helper -> failure(helper, kind, failure)));
                }
            }
            return tests;
        }

        static void failure(final GameTestHelper helper, final String kind, final String failure) {
            final var f = new Fixture(helper, kind);
            final Value[] request = {null};
            final Value[] alias = {null};
            final var sequence = helper.startSequence().thenWaitUntil(() -> {
                final var host = helper.getLevel().getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, f.drive, null);
                helper.assertTrue(host != null && host.getGridNode(Direction.UP) != null && host.getGridNode(Direction.UP).isActive(), "Waiting for failure grid");
                f.grid = host.getGridNode(Direction.UP).getGrid();
                helper.assertTrue(f.grid.getCraftingService().getCpus().size() == 2 && f.grid.getCraftingService().isCraftable(appeng.api.stacks.AEItemKey.of(Items.GOLD_INGOT)),
                    "Waiting for two actual CPUs and processing recipe");
                helper.assertTrue(f.grid.getCraftingService().getCpus().stream().anyMatch(cpu -> cpu.getName() != null && cpu.getName().getString().equals("selected-cpu")), "Waiting for selected CPU name");
                helper.assertTrue(f.grid.getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 16,
                    appeng.api.config.Actionable.SIMULATE, appeng.api.networking.security.IActionSource.empty()) == 16, "Waiting for actual storage mount");
            }).thenExecute(() -> {
                helper.assertTrue(f.grid.getStorageService().getInventory().insert(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 16,
                    appeng.api.config.Actionable.MODULATE, appeng.api.networking.security.IActionSource.empty()) == 16, "Cannot seed failure ingredients");
                try {
                    final var environment = li.cil.oc.api.Driver.driverFor(helper.getLevel(), f.host, Direction.SOUTH).createEnvironment(helper.getLevel(), f.host, Direction.SOUTH);
                    try {
                        final var recipe = (Value) ((Object[]) ((li.cil.oc.api.network.Component) environment.node()).invoke("getCraftables", null, Map.of("name", "minecraft:gold_ingot"))[0])[0];
                        request[0] = (Value) f.computer.machine().invoke(recipe, "request", new Object[]{1, true, "selected-cpu"})[0];
                        final var saved = new CompoundTag(); request[0].save(saved);
                        alias[0] = new li.cil.oc.common.integration.ae2.CraftingStatusValue(); alias[0].load(saved);
                        helper.assertTrue(f.state(request[0], "isDone")[0] == null && !saved.contains("link"), "Fresh failure request already submitted");
                    } finally { environment.node().remove(); }
                } catch (Exception exception) { helper.fail(exception.toString()); }
            });
            if (failure.equals("submitted_host_removed")) {
                sequence.thenWaitUntil(() -> {
                    final var saved = new CompoundTag(); request[0].save(saved);
                    helper.assertTrue(saved.contains("link"), "Waiting for actual submitted processing request: " + java.util.Arrays.toString(f.state(request[0], "isDone")));
                    helper.assertTrue(f.state(request[0], "isDone").length == 1 && Boolean.FALSE.equals(f.state(request[0], "isDone")[0]), "Processing request not pending");
                });
            }
            if (failure.startsWith("cpu_")) {
                sequence.thenIdle(1).thenExecute(() -> {
                    final var saved = new CompoundTag(); request[0].save(saved);
                    helper.assertTrue(!saved.getCompound("node").isEmpty() && !saved.contains("link") && f.state(request[0], "isDone")[0] == null,
                        "CPU mutation must happen after calculation starts and before submission");
                });
            }
            sequence.thenExecute(() -> {
                if (failure.startsWith("cpu_")) {
                    helper.getLevel().setBlockAndUpdate(f.selectedCpu, Blocks.AIR.defaultBlockState());
                    if (failure.equals("cpu_replaced")) { f.put(f.selectedCpu, "1k_crafting_storage"); f.nameCpu(f.selectedCpu, "selected-cpu"); }
                }
                else {
                    helper.getLevel().setBlockAndUpdate(f.host, Blocks.AIR.defaultBlockState());
                    if (failure.equals("host_replaced")) f.placeHost();
                }
            }).thenWaitUntil(() -> {
                final var state = f.state(request[0], "isDone");
                final String reason = failure.startsWith("cpu_") ? "request failed (cpu_offline)" : "request failed (no controller)";
                helper.assertTrue(state.length == 2 && Boolean.FALSE.equals(state[0]) && reason.equals(state[1]), "Wrong failure outcome: " + java.util.Arrays.toString(state));
                for (final var value : List.of(request[0], alias[0])) {
                    final var canceled = f.state(value, "isCanceled");
                    helper.assertTrue(canceled.length == 2 && Boolean.FALSE.equals(canceled[0]) && reason.equals(canceled[1]), "Failed state/cancellation differs between aliases");
                    final var cancel = f.state(value, "cancel");
                    helper.assertTrue(cancel.length == 2 && Boolean.FALSE.equals(cancel[0]) && reason.equals(cancel[1]), "Cancel concealed failure state");
                }
            }).thenWaitUntil(() -> {
                final var contents = f.grid.getStorageService().getInventory().getAvailableStacks();
                final var chest = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(f.output);
                int external = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    final var stack = chest.getItem(slot);
                    helper.assertTrue(stack.isEmpty() || stack.is(Items.OAK_LOG), "Unexpected external crafting output");
                    external += stack.getCount();
                }
                helper.assertTrue(contents.get(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG)) + external == 16
                    && contents.get(appeng.api.stacks.AEItemKey.of(Items.GOLD_INGOT)) == 0, "Failed request lost/duplicated ingredients or fabricated output");
                helper.assertTrue(f.grid.getCraftingService().getCpus().stream().noneMatch(appeng.api.networking.crafting.ICraftingCPU::isBusy), "Failed request retained a CPU or silently selected backup CPU");
                if (!failure.equals("submitted_host_removed")) helper.assertTrue(external == 0, "Pre-submission failure consumed ingredients");
            }).thenSucceed();
        }

        static final class Fixture {
            final GameTestHelper helper;
            final String kind;
            final BlockPos host, drive, selectedCpu, output;
            final ComputerCaseBlockEntity computer;
            appeng.api.networking.IGrid grid;

            Fixture(final GameTestHelper helper, final String kind) {
                this.helper = helper; this.kind = kind;
                host = helper.absolutePos(new BlockPos(3, 3, 3)); drive = host.east(); selectedCpu = drive.north(); output = drive.south().above();
                placeHost();
                put(drive, "drive"); put(host.north(), "creative_energy_cell");
                put(selectedCpu, "1k_crafting_storage"); put(host.north(2), "1k_crafting_storage");
                // Keep storage/CPUs powered even after the tested host is removed, and avoid the drive's front.
                for (final var cable : List.of(drive.east(), selectedCpu.east(), selectedCpu.north(), selectedCpu.east().north())) {
                    appeng.api.parts.PartHelper.setPart(helper.getLevel(), cable, null, null,
                        (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(id("fluix_glass_cable")));
                }
                nameCpu(selectedCpu, "selected-cpu"); nameCpu(host.north(2), "backup-cpu");
                final var providerPos = drive.south(); put(providerPos, "pattern_provider");
                helper.getLevel().setBlockAndUpdate(output, Blocks.CHEST.defaultBlockState());
                final var pattern = appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern(
                    List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.OAK_LOG), 1)),
                    List.of(new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(Items.GOLD_INGOT), 1)));
                final var provider = helper.getLevel().getBlockEntity(providerPos);
                final var data = provider.saveWithFullMetadata(helper.getLevel().registryAccess());
                final var patterns = new net.minecraft.nbt.ListTag();
                final var encoded = (CompoundTag) pattern.save(helper.getLevel().registryAccess()); encoded.putInt("Slot", 0); patterns.add(encoded);
                data.put("patterns", patterns); provider.loadWithComponents(data, helper.getLevel().registryAccess());
                final var cells = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, drive, Direction.UP);
                helper.assertTrue(cells != null && cells.insertItem(0, new ItemStack(BuiltInRegistries.ITEM.get(id("item_storage_cell_1k"))), false).isEmpty(), "Failure fixture rejected storage cell");
                helper.getLevel().setBlockAndUpdate(host.west(), ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
                computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(host.west());
            }

            void placeHost() {
                if (!kind.equals("panel")) put(host, kind);
                else {
                    appeng.api.parts.PartHelper.setPart(helper.getLevel(), host, null, null, (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(id("fluix_glass_cable")));
                    helper.assertTrue(appeng.api.parts.PartHelper.setPart(helper.getLevel(), host, Direction.NORTH, null,
                        (appeng.api.parts.IPartItem<?>) BuiltInRegistries.ITEM.get(id("cable_interface"))) != null, "Cannot install failure interface panel");
                }
            }
            void put(final BlockPos pos, final String block) { helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id(block)).defaultBlockState()); }
            void nameCpu(final BlockPos pos, final String name) {
                ((appeng.blockentity.crafting.CraftingBlockEntity) helper.getLevel().getBlockEntity(pos)).setName(name);
            }
            Object[] state(final Value value, final String method) {
                try { return computer.machine().invoke(value, method, new Object[0]); }
                catch (Exception exception) { helper.fail(exception.toString()); return new Object[0]; }
            }
        }

        static ResourceLocation id(final String path) { return ResourceLocation.fromNamespaceAndPath("ae2", path); }
    }
}
