package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.component.ChunkloaderUpgradeEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Collection;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class ChunkloaderDimensionGameTests {
    private static final BlockPos RESTART_OWNER = new BlockPos(4, 80, 4);
    private static final BlockPos RESTART_RECORD = new BlockPos(2, 80, 2);

    @GameTestGenerator
    public static Collection<TestFunction> chunkloaderPolicyRestartProbe() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty()) return List.of();
        if (phase.equals("verify")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) -> {
                    ModSettings.CHUNKLOADER_DIMENSION_BLACKLIST.set(List.of("minecraft:the_nether"));
                    NeoOpenComputers.LOGGER.info("Chunkloader restart fixture blocks Nether before worlds load in PID {}", ProcessHandle.current().pid());
                });
        }
        return List.of(new TestFunction("processRestart", "chunkloaderpolicyrestart_" + phase, "neoopencomputers:empty", 400, 0, true,
            helper -> restart(helper, phase.equals("prepare"))));
    }

    private static void restart(final GameTestHelper helper, final boolean prepare) {
        final var world = helper.getLevel();
        final var nether = world.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "Restart fixture requires Nether");
        world.setChunkForced(0, 0, true);
        if (prepare) {
            helper.assertTrue(world.isEmptyBlock(RESTART_RECORD), "Chunkloader checkpoint exists; use a fresh world");
            world.setBlockAndUpdate(RESTART_RECORD, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(world.getBlockEntity(RESTART_RECORD) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
            "Missing saved chunkloader record");
        final var record = (net.minecraft.world.level.block.entity.ChestBlockEntity) world.getBlockEntity(RESTART_RECORD);
        if (prepare) {
            final var host = new Host(nether, RESTART_OWNER);
            final var environment = environment(host);
            final var component = (ComponentConnector) environment.node();
            try {
                component.invoke("setActive", null, true);
            } catch (Exception exception) {
                component.remove();
                throw new IllegalStateException(exception);
            }
            assertTickets(helper, nether, RESTART_OWNER, true);
            // Keep these real tickets until normal shutdown so the second process must validate them.
        } else {
            final var data = record.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && data.copyTag().getString("phase").equals("prepared")
                && data.copyTag().getLong("pid") > 0 && data.copyTag().getLong("pid") != ProcessHandle.current().pid(),
                "Chunkloader checkpoint was not prepared by another process");
            helper.assertTrue(ModSettings.CHUNKLOADER_DIMENSION_BLACKLIST.get().contains("minecraft:the_nether"),
                "Policy was not changed before world loading");
            // Do not instantiate/update a loader here: startup validation alone must remove the saved tickets.
            assertTickets(helper, nether, RESTART_OWNER, false);
            NeoOpenComputers.LOGGER.info("Chunkloader saved tickets rejected on world load; prepare PID {}, verify PID {}",
                data.copyTag().getLong("pid"), ProcessHandle.current().pid());
        }
        final var tag = new CompoundTag();
        tag.putString("phase", prepare ? "prepared" : "verified");
        tag.putLong("pid", ProcessHandle.current().pid());
        final var paper = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        paper.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        record.setItem(0, paper); record.setChanged();
        NeoOpenComputers.LOGGER.info("Chunkloader policy restart probe {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkloaderDimensionListsAndBlacklistPrecedence(final GameTestHelper helper) throws Exception {
        withPolicy(List.of(), List.of(), () -> {
            final var host = new Host(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
            final var environment = environment(host);
            final var component = (ComponentConnector) environment.node();
            try {
                helper.assertTrue(Boolean.TRUE.equals(component.invoke("setActive", null, true)[0]), "Empty policy denied activation");
                assertTickets(helper, host.level, host.pos, true);
                helper.assertTrue(Boolean.FALSE.equals(component.invoke("setActive", null, true)[0]), "Repeated enable changed activation");
                component.invoke("setActive", null, false);
                assertTickets(helper, host.level, host.pos, false);
                withPolicy(List.of("minecraft:the_nether"), List.of(), () -> assertDenied(helper, component));
                withPolicy(List.of("minecraft:overworld"), List.of("minecraft:overworld"), () -> assertDenied(helper, component));
                assertTickets(helper, host.level, host.pos, false);
                withPolicy(List.of("minecraft:overworld"), List.of("minecraft:the_nether"), () -> {
                    component.invoke("setActive", null, true);
                    assertTickets(helper, host.level, host.pos, true);
                });
                final double energy = component.localBuffer();
                withPolicy(List.of(), List.of("minecraft:overworld"), () -> {
                    environment.update();
                    helper.assertTrue(Boolean.FALSE.equals(component.invoke("isActive", null)[0]), "Policy change left loader active");
                    assertTickets(helper, host.level, host.pos, false);
                    helper.assertTrue(component.localBuffer() == energy, "Blocked loader consumed energy");
                    final var saved = new CompoundTag(); environment.save(saved);
                    helper.assertTrue(!saved.getBoolean("active"), "Blocked loader persisted active state");
                });
            } finally {
                component.invoke("setActive", null, false);
                component.remove();
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkloaderBlockedRestoreAndAutomaticStartDoNotAcquireTickets(final GameTestHelper helper) throws Exception {
        withPolicy(List.of(), List.of("minecraft:overworld"), () -> {
            final var host = new Host(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
            final var environment = environment(host);
            final var component = (ComponentConnector) environment.node();
            try {
                final var saved = new CompoundTag(); saved.putBoolean("active", true);
                environment.load(saved);
                Network.joinNewNetwork(component);
                environment.onMessage(new LifecycleMessage(component, "computer.started"));
                helper.assertTrue(Boolean.FALSE.equals(component.invoke("isActive", null)[0]), "Blocked restore/start became active");
                assertTickets(helper, host.level, host.pos, false);
                assertDenied(helper, component);
                withPolicy(List.of(), List.of(), () -> {
                    environment.update();
                    helper.assertTrue(Boolean.FALSE.equals(component.invoke("isActive", null)[0]), "Policy relaxation resurrected rejected activation");
                    component.invoke("setActive", null, true);
                    assertTickets(helper, host.level, host.pos, true);
                });
            } finally {
                component.invoke("setActive", null, false);
                component.remove();
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkloaderDimensionChangeReleasesOriginalWorldTickets(final GameTestHelper helper) throws Exception {
        withPolicy(List.of(), List.of(), () -> {
            final var overworld = helper.getLevel();
            final var nether = overworld.getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Missing Nether fixture");
            final var host = new Host(overworld, helper.absolutePos(new BlockPos(1, 1, 1)));
            final var environment = environment(host);
            final var component = (ComponentConnector) environment.node();
            try {
                component.invoke("setActive", null, true);
                assertTickets(helper, overworld, host.pos, true);
                host.level = nether;
                environment.update();
                assertTickets(helper, overworld, host.pos, false);
                assertTickets(helper, nether, host.pos, true);
                host.level = overworld;
                withPolicy(List.of(), List.of("minecraft:overworld"), () -> {
                    environment.update();
                    assertTickets(helper, nether, host.pos, false);
                    assertTickets(helper, overworld, host.pos, false);
                    helper.assertTrue(Boolean.FALSE.equals(component.invoke("isActive", null)[0]), "Blocked world transition stayed active");
                });
            } finally {
                host.level = overworld;
                component.invoke("setActive", null, false);
                component.remove();
            }
        });
        helper.succeed();
    }

    private static ChunkloaderUpgradeEnvironment environment(final Host host) {
        final var environment = new ChunkloaderUpgradeEnvironment(host);
        final var component = (ComponentConnector) environment.node();
        component.setLocalBufferSize(64);
        component.changeBuffer(64);
        Network.joinNewNetwork(component);
        return environment;
    }

    private static void assertDenied(final GameTestHelper helper, final ComponentConnector component) throws Exception {
        try {
            component.invoke("setActive", null, true);
        } catch (Exception exception) {
            helper.assertTrue("this dimension is blacklisted".equals(exception.getMessage()), "Wrong denied-dimension error: " + exception);
            helper.assertTrue(Boolean.FALSE.equals(component.invoke("isActive", null)[0]), "Rejected activation changed state");
            return;
        }
        helper.fail("Chunkloader accepted blocked dimension");
    }

    private static void withPolicy(final List<String> whitelist, final List<String> blacklist,
                                   final NeoOpenComputersGameTests.ThrowingRunnable action) throws Exception {
        NeoOpenComputersGameTests.withCachedConfig(ModSettings.CHUNKLOADER_DIMENSION_WHITELIST, whitelist, () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.CHUNKLOADER_DIMENSION_BLACKLIST, blacklist, action));
    }

    private static void assertTickets(final GameTestHelper helper, final ServerLevel level, final BlockPos owner, final boolean expected) {
        final var data = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), ForcedChunksSavedData.FILE_ID)
            .save(new CompoundTag(), level.registryAccess());
        final var found = new java.util.HashSet<Long>();
        for (var entry : data.getList("ModForced", Tag.TAG_COMPOUND)) {
            final var controller = (CompoundTag) entry;
            if (!controller.getString("Controller").equals(NeoOpenComputers.MODID + ":chunkloader_upgrade")) continue;
            for (var ticketEntry : controller.getList("ModForced", Tag.TAG_COMPOUND)) {
                final var ticket = (CompoundTag) ticketEntry;
                for (var blockEntry : ticket.getList("TickingBlocks", Tag.TAG_COMPOUND)) {
                    final var block = (CompoundTag) blockEntry;
                    if (block.getInt("X") == owner.getX() && block.getInt("Y") == owner.getY() && block.getInt("Z") == owner.getZ()) {
                        found.add(ticket.getLong("Chunk"));
                    }
                }
            }
        }
        final var center = new ChunkPos(owner);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            helper.assertTrue(found.contains(ChunkPos.asLong(center.x + x, center.z + z)) == expected,
                "Wrong ticket state in " + level.dimension().location() + " at offset " + x + "," + z);
        }
    }

    private static final class Host implements EnvironmentHost {
        private ServerLevel level;
        private final BlockPos pos;
        Host(final ServerLevel level, final BlockPos pos) { this.level = level; this.pos = pos; }
        @Override public Level world() { return level; }
        @Override public double xPosition() { return pos.getX(); }
        @Override public double yPosition() { return pos.getY(); }
        @Override public double zPosition() { return pos.getZ(); }
        @Override public void markChanged() { }
    }

    private record LifecycleMessage(Node source, String name) implements Message {
        @Override public Object[] data() { return new Object[0]; }
        @Override public void cancel() { }
    }
}
