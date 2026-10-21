package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DisassemblerParityGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 1);

    @GameTest(template = "empty")
    public static void disassemblerAutomaticallyQueuesInsertedInput(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            final var machine = machine(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
            tick(helper, machine);
            helper.assertTrue(machine.getItem(0).isEmpty() && queue(helper, machine) == 1 && !machine.containsOutput(Items.EMERALD),
                "Inserted disassembler input did not automatically become an unpaid queued job"); helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerCannotReplaceAnExistingQueue(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            final var machine = queued(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
            helper.assertTrue(!machine.disassemble(), "Starting a second disassembly overwrote an existing job");
            helper.assertTrue(queue(helper, machine) == 1 && machine.getItem(0).is(Items.DIAMOND), "Rejected second start changed queued work/input");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerInstantPathCannotBypassExistingQueue(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            final var machine = queued(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
            helper.assertTrue(!machine.disassemble(RandomSource.create(0L), 0D), "Instant path accepted a new job while old work was queued");
            helper.assertTrue(queue(helper, machine) == 1 && machine.getItem(0).is(Items.DIAMOND) && !machine.containsOutput(Items.EMERALD), "Instant rejection changed input/queue/output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerInvokesItsProviderOncePerStart(GameTestHelper helper) {
        final int[] calls = {0};
        try (var ignored = DisassemblerTemplates.register(new DisassemblerTemplate() {
            public String name() { return "single_start_probe"; }
            public boolean matches(ItemStack stack) { return stack.is(Items.DIAMOND); }
            public ItemStack[] disassemble(ItemStack stack) { calls[0]++; return new ItemStack[]{new ItemStack(Items.EMERALD)}; }
        })) {
            final var machine = machine(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
            helper.assertTrue(machine.disassemble() && calls[0] == 1, "One start invoked the addon provider more than once"); helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerHopperDoesNotStealQueuedInput(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            helper.setBlock(POS.below(), Blocks.HOPPER); final HopperBlockEntity hopper = helper.getBlockEntity(POS.below());
            final var machine = machine(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
            helper.assertTrue(!HopperBlockEntity.suckInItems(helper.getLevel(), hopper) && hopper.isEmpty() && machine.getItem(0).is(Items.DIAMOND), "Hopper stole valid disassembler input");
            machine.setItem(1, new ItemStack(Items.EMERALD));
            helper.assertTrue(HopperBlockEntity.suckInItems(helper.getLevel(), hopper) && hopper.getItem(0).is(Items.EMERALD) && machine.getItem(0).is(Items.DIAMOND), "Hopper did not extract only completed output"); helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerItemHandlerAcceptsInputAndProtectsIt(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            final var machine = machine(helper);
            final Direction[] sides = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (var side : sides) {
                final var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, machine.getBlockPos(), side);
                helper.assertTrue(handler != null && handler.getSlots() == DisassemblerBlockEntity.CONTAINER_SIZE, "Disassembler item capability missing on " + side);
                helper.assertTrue(handler.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty() && handler.extractItem(0, 1, false).isEmpty(), "Automation failed insertion or stole input");
                helper.assertTrue(handler.insertItem(1, new ItemStack(Items.EMERALD), false).is(Items.EMERALD), "Automation inserted into completed-output slot");
                machine.setItem(1, new ItemStack(Items.EMERALD));
                helper.assertTrue(handler.extractItem(1, 1, false).is(Items.EMERALD), "Automation could not extract completed output"); machine.setItem(0, ItemStack.EMPTY);
            }
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerRejoinsAnExistingCableAfterLoad(GameTestHelper helper) {
        helper.setBlock(POS.west(), ModBlocks.CABLE.get()); final CableBlockEntity cable = helper.getBlockEntity(POS.west());
        final var machine = machine(helper); final String address = machine.node().address();
        final var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess()); machine.onChunkUnloaded(); machine.setRemoved();
        final var restored = (DisassemblerBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored); restored.onLoad();
        helper.assertTrue(restored.node().network() != null && restored.node().network() == cable.node().network() && restored.node().address().equals(address), "Reloaded disassembler lost cable connection/address"); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRespectsAdjacentFurnaceInsertionFace(GameTestHelper helper) {
        try (var ignored = recipe(Items.IRON_INGOT)) {
            final var machine = machine(helper); helper.setBlock(POS.above(), Blocks.FURNACE);
            final FurnaceBlockEntity furnace = helper.getBlockEntity(POS.above());
            furnace.setItem(0, new ItemStack(Items.COBBLESTONE, 64)); furnace.setItem(1, new ItemStack(Items.COAL, 64)); furnace.setItem(2, new ItemStack(Items.IRON_INGOT));
            machine.setItem(0, new ItemStack(Items.DIAMOND));
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D), "Fixture disassembly failed");
            helper.assertTrue(furnace.getItem(2).getCount() == 1 && machine.containsOutput(Items.IRON_INGOT), "Disassembler inserted into furnace output through a forbidden face"); helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerHonorsUpstreamUndisassemblableFlag(GameTestHelper helper) {
        try (var ignored = recipe(Items.EMERALD)) {
            final var machine = machine(helper); final var stack = new ItemStack(Items.DIAMOND);
            final var tag = new CompoundTag(); tag.putBoolean("oc:undisassemblable", true); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            helper.assertTrue(!machine.canInsert(stack) && DisassemblerTemplates.select(stack).isEmpty(), "Undisassemblable input was accepted despite the upstream opt-out"); helper.succeed();
        }
    }

    private static DisassemblerBlockEntity machine(GameTestHelper helper) { helper.setBlock(POS, ModBlocks.DISASSEMBLER.get()); return helper.getBlockEntity(POS); }
    private static DisassemblerBlockEntity queued(GameTestHelper helper) { final var machine = machine(helper); machine.setItem(0, new ItemStack(Items.DIAMOND)); helper.assertTrue(machine.disassemble(), "Fixture did not queue work"); return machine; }
    private static void tick(GameTestHelper helper, DisassemblerBlockEntity machine) { DisassemblerBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine); }
    private static int queue(GameTestHelper helper, DisassemblerBlockEntity machine) { return machine.saveWithFullMetadata(helper.getLevel().registryAccess()).getList("oc:queue", 10).size(); }
    private static DisassemblerTemplates.Registration recipe(net.minecraft.world.item.Item output) {
        return DisassemblerTemplates.register(new DisassemblerTemplate() {
            public String name() { return "disassembler_parity_probe"; }
            public boolean matches(ItemStack stack) { return stack.is(Items.DIAMOND); }
            public ItemStack[] disassemble(ItemStack stack) { return new ItemStack[]{new ItemStack(output)}; }
        });
    }
}
