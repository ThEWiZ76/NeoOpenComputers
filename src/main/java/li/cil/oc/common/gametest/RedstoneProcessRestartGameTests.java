package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Adds redstone acceptance to the existing isolated two-process probe when CC is installed. */
@GameTestHolder(NeoOpenComputers.MODID)
public final class RedstoneProcessRestartGameTests {
    private static final List<BlockPos> HOSTS = List.of(new BlockPos(10, 80, 10), new BlockPos(14, 80, 10),
        new BlockPos(10, 80, 14), new BlockPos(14, 80, 14));
    private static final BlockPos RECORD = new BlockPos(10, 80, 6);
    private static final Map<BlockPos, CompoundTag> DISK_INPUTS = new ConcurrentHashMap<>();

    @GameTestGenerator
    public static Collection<TestFunction> redstoneRestartProbe() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty() || !net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        if (!phase.equals("prepare") && !phase.equals("verify")) throw new IllegalArgumentException("Unknown restart phase");
        if (phase.equals("verify")) {
            NeoForge.EVENT_BUS.addListener((ChunkDataEvent.Load event) -> {
                // This event is async and precedes ticking: copy NBT only, never query/load a world here.
                if (event.getChunk().getPos().x != 0 || event.getChunk().getPos().z != 0) return;
                final var entities = event.getData().getList("block_entities", Tag.TAG_COMPOUND);
                for (int index = 0; index < entities.size(); index++) {
                    final var data = entities.getCompound(index);
                    final var pos = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                    if (HOSTS.contains(pos) && data.getString("id").startsWith(NeoOpenComputers.MODID + ":")) {
                        DISK_INPUTS.putIfAbsent(pos, data.copy());
                    }
                }
            });
        }
        // Share the native probe's embedded player/chunk ticking, including source computer startup.
        return List.of(new TestFunction("processRestart", "redstonerestart_" + phase, "neoopencomputers:empty", 4000, 0, true,
            helper -> run(helper, phase.equals("prepare"))));
    }

    private static void run(GameTestHelper helper, boolean prepare) {
        final var world = helper.getLevel();
        world.setChunkForced(0, 0, true);
        if (prepare) {
            helper.assertTrue(world.isEmptyBlock(RECORD) && HOSTS.stream().allMatch(world::isEmptyBlock), "Redstone checkpoint already exists; use a fresh isolated world");
            world.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
            final var states = List.of(ModBlocks.COMPUTER_CASE_TIER2.get().defaultBlockState(), ModBlocks.MICROCONTROLLER_TIER2.get().defaultBlockState(),
                ModBlocks.ROBOT.get().defaultBlockState(), ModBlocks.REDSTONE_IO.get().defaultBlockState());
            for (int index = 0; index < HOSTS.size(); index++) {
                final var pos = HOSTS.get(index);
                world.setBlockAndUpdate(pos, states.get(index));
                if (world.getBlockEntity(pos) instanceof RobotBlockEntity robot) {
                    robot.setTier(1);
                    RobotMovementPersistenceGameTests.installHardware(helper, robot, List.of(new ItemStack(ModItems.REDSTONE_CARD_TIER2.get())));
                }
                try {
                    RedstoneCardGameTests.bootComputerCraft(helper, pos.above(),
                        "redstone.setAnalogOutput('bottom', 15); redstone.setBundledOutput('bottom', 32773)", true);
                } catch (Exception exception) { throw new RuntimeException(exception); }
            }
        }
        helper.assertTrue(world.getBlockEntity(RECORD) instanceof ChestBlockEntity, "Saved redstone checkpoint is missing");
        final var record = (ChestBlockEntity) world.getBlockEntity(RECORD);
        final var savedRecord = record.getItem(0).get(DataComponents.CUSTOM_DATA);
        if (!prepare) {
            helper.assertTrue(savedRecord != null && savedRecord.copyTag().getString("phase").equals("prepared")
                && savedRecord.copyTag().getLong("pid") > 0 && savedRecord.copyTag().getLong("pid") != ProcessHandle.current().pid(),
                "Redstone checkpoint was not prepared by a different process");
            for (int index = 0; index < HOSTS.size(); index++) {
                final var pos = HOSTS.get(index);
                final var raw = DISK_INPUTS.get(pos);
                helper.assertTrue(raw != null, "Chunk loader did not capture saved redstone NBT before ticking: " + pos);
                final var vanilla = raw.getIntArray("oc:redstoneInputs");
                final var bundled = raw.getIntArray("oc:bundledRedstoneInputs");
                helper.assertTrue(vanilla.length == 6 && vanilla[1] == 15 && bundled.length == 96
                    && bundled[16] == 15 && bundled[18] == 15 && bundled[31] == 15 && bundled[17] == 0,
                    "Disk data lost sampled input history for host " + index);
                helper.assertTrue(node(world.getBlockEntity(pos)).address().equals(savedRecord.copyTag().getString("node" + index)),
                    "Redstone host node identity changed across the process restart");
            }
            NeoOpenComputers.LOGGER.info("Redstone restart probe inspected disk input arrays before ticking for all four hosts; prepare PID {}, verify PID {}",
                savedRecord.copyTag().getLong("pid"), ProcessHandle.current().pid());
        }
        helper.startSequence().thenWaitUntil(() -> {
            for (int index = 0; index < HOSTS.size(); index++) {
                final var blockEntity = world.getBlockEntity(HOSTS.get(index));
                helper.assertTrue(blockEntity instanceof RedstoneControllerHost, "Saved redstone host is missing: " + index);
                final var host = (RedstoneControllerHost) blockEntity;
                helper.assertTrue(host.redstoneInput(Direction.UP) == 15 && host.bundledRedstoneInput(Direction.UP, 15) == 15,
                    "Real CC source is not ready after restart: host " + index + ", vanilla=" + host.redstoneInput(Direction.UP)
                        + ", bundled=" + host.bundledRedstoneInput(Direction.UP, 15)
                        + ", chunkTicking=" + world.shouldTickBlocksAt(new net.minecraft.world.level.ChunkPos(HOSTS.get(index)).toLong())
                        + ", sourceBundled=" + dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(world, HOSTS.get(index).above(), Direction.DOWN));
            }
        }).thenExecute(() -> {
            final var data = new CompoundTag();
            data.putLong("pid", ProcessHandle.current().pid());
            data.putString("phase", prepare ? "prepared" : "verified");
            for (int index = 0; index < HOSTS.size(); index++) {
                final var blockEntity = world.getBlockEntity(HOSTS.get(index));
                final var host = (RedstoneControllerHost) blockEntity;
                if (prepare) {
                    host.setWakeThreshold(8);
                    host.setRedstoneOutput(Direction.WEST, 9);
                    host.setBundledRedstoneOutput(Direction.EAST, 7, 42);
                } else {
                    helper.assertTrue(host.wakeThreshold() == 8 && host.redstoneOutput(Direction.WEST) == 9
                        && host.bundledRedstoneOutput(Direction.EAST, 7) == 42, "Restart lost redstone output or wake threshold: host " + index);
                }
                data.putString("node" + index, node(blockEntity).address());
            }
            if (!prepare) verifyIoEvents(helper);
            final var paper = new ItemStack(Items.PAPER);
            paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            record.setItem(0, paper);
            record.setChanged();
            NeoOpenComputers.LOGGER.info("Redstone restart probe {} in PID {}; world checkpoint retained for inspection",
                prepare ? "prepared" : "verified", ProcessHandle.current().pid());
        }).thenSucceed();
    }

    private static void verifyIoEvents(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = HOSTS.get(3);
        final var io = (RedstoneIoBlockEntity) world.getBlockEntity(pos);
        final var events = new java.util.ArrayList<Object[]>();
        final var listener = new AbstractManagedEnvironment() {
            { setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).create()); }
            @Override public void onMessage(li.cil.oc.api.network.Message message) {
                if (message.name().equals("computer.signal")) events.add(message.data());
            }
        };
        helper.assertTrue(io.node().network() != null, "IO network was not recreated after process restart");
        io.node().connect(listener.node());
        try {
            io.updateRedstoneInputs();
            helper.assertTrue(events.isEmpty(), "Unchanged IO sampling emitted events after process restart");
            world.removeBlock(pos.above(), false);
            io.updateRedstoneInputs();
            helper.assertTrue(events.size() == 4, "Restarted IO did not deliver the three bundled and one vanilla falling events");
            for (int index = 0; index < 3; index++) helper.assertTrue(java.util.Arrays.equals(events.get(index),
                new Object[]{"redstone_changed", 1, 15, 0, new int[]{0, 2, 15}[index]}), "Restarted bundled event lost its input history");
            helper.assertTrue(java.util.Arrays.equals(events.get(3), new Object[]{"redstone_changed", 1, 15, 0}), "Restarted vanilla event lost its input history");
            io.updateRedstoneInputs();
            helper.assertTrue(events.size() == 4, "Restarted IO duplicated stable input events");
        } finally { listener.node().remove(); }
    }

    private static Node node(net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (entity instanceof ComputerCaseBlockEntity host) return host.machine().node();
        if (entity instanceof MicrocontrollerBlockEntity host) return host.machine().node();
        if (entity instanceof RobotBlockEntity host) return host.node();
        return ((RedstoneIoBlockEntity) entity).node();
    }
}
