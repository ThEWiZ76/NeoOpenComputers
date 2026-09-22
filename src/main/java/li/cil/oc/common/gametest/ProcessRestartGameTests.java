package li.cil.oc.common.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Opt-in two-process probe. Only touches the separate world selected by the Gradle properties. */
@GameTestHolder(NeoOpenComputers.MODID)
public final class ProcessRestartGameTests {
    private static final BlockPos COMPUTER = new BlockPos(6, 80, 6);
    private static final BlockPos DRIVE = COMPUTER.east();
    private static final BlockPos RECORD = COMPUTER.south(2);
    private static final UUID PLAYER_ID = UUID.fromString("b89325ac-405a-4484-8a73-10a217e22e31");

    @GameTestGenerator
    public static Collection<TestFunction> restartProbe() {
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (phase.isEmpty()) return List.of();
        if (!phase.equals("prepare") && !phase.equals("verify")) throw new IllegalArgumentException("Unknown restart phase");
        // The forced chunk may resume before this probe's batch starts. Supply test
        // power from the first world tick, not only when the assertions begin.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level != null && level.hasChunkAt(COMPUTER) && level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity computer) {
                ((Connector) computer.machine().node()).changeBuffer(10000);
            }
        });
        return List.of(new TestFunction("processRestart", "processrestart_" + phase, "neoopencomputers:empty", 400, 0, true,
            helper -> run(helper, phase.equals("prepare"))));
    }

    private static void run(GameTestHelper helper, boolean prepare) {
        final var level = helper.getLevel();
        level.setChunkForced(0, 0, true);
        if (prepare) {
            helper.assertTrue(level.isEmptyBlock(COMPUTER) && level.isEmptyBlock(RECORD), "Probe world already contains a checkpoint; use a fresh world");
            level.setBlockAndUpdate(COMPUTER, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            level.setBlockAndUpdate(DRIVE, ModBlocks.DISK_DRIVE.get().defaultBlockState());
            level.setBlockAndUpdate(RECORD, Blocks.CHEST.defaultBlockState());
        }
        helper.assertTrue(level.getBlockEntity(COMPUTER) instanceof ComputerCaseBlockEntity, "Saved computer missing after process restart");
        helper.assertTrue(level.getBlockEntity(DRIVE) instanceof DiskDriveBlockEntity, "Saved disk drive missing after process restart");
        helper.assertTrue(level.getBlockEntity(RECORD) instanceof ChestBlockEntity, "Saved process record missing");
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(COMPUTER);
        final var disk = (DiskDriveBlockEntity) level.getBlockEntity(DRIVE);
        final var record = (ChestBlockEntity) level.getBlockEntity(RECORD);
        final var player = connectPlayer(helper);
        if (prepare) {
            disk.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu());
            computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER2.get()));
            computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                assert(eeprom.getData() == '', 'unexpected reboot')
                local retained = 731
                local drive = component.proxy(component.list('disk_drive')())
                local fs = component.proxy(assert(drive.media()))
                local file = assert(fs.open('restart.bin', 'w'))
                assert(fs.write(file, 'a' .. string.char(0,255) .. 'z'))
                fs.close(file)
                file = assert(fs.open('restart.bin', 'r'))
                assert(fs.read(file, 1) == 'a')
                eeprom.setData('waiting for process restart')
                repeat until computer.pullSignal() == 'after_process_restart'
                assert(retained == 731, 'Lua local lost')
                assert(drive.media() == fs.address, 'external filesystem address changed')
                assert(fs.read(file, 3) == string.char(0,255) .. 'z', 'external file offset lost')
                fs.close(file)
                eeprom.setData('resumed after process restart')
                while true do computer.pullSignal() end
                """));
            helper.assertTrue(computer.toggleMachine(), "Probe computer did not start");
            final var tablet = ModItems.TABLET.get().assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
                cpu(), new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("""
                    local eeprom = component.proxy(component.list('eeprom')())
                    eeprom.setData('tablet checkpoint')
                    while true do computer.pullSignal() end
                    """));
            player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
            helper.assertTrue(TabletRuntimeRegistry.get(tablet, player).start(), "Probe tablet did not start");
        } else {
            final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null && data.copyTag().getString("phase").equals("prepared")
                && data.copyTag().getLong("pid") > 0 && data.copyTag().getLong("pid") != ProcessHandle.current().pid(), "Checkpoint was not prepared by another process");
            final ItemStack tablet = player.getMainHandItem();
            helper.assertTrue(tablet.is(ModItems.TABLET.get()), "Player tablet was not loaded from disk");
            helper.assertTrue(marker(ModItems.TABLET.get().getComponent(tablet, 3)).equals("tablet checkpoint"), "Player file lost newest tablet data");
            helper.assertTrue(!TabletRuntimeRegistry.get(tablet, player).machine().isRunning(), "Orderly logout tablet unexpectedly resumed");
            helper.assertTrue(computer.machine().isRunning(), "World computer lost running state: " + computer.machine().lastError());
            helper.assertTrue(computer.machine().signal("after_process_restart"), "Restart signal rejected");
        }
        // Embedded connections need explicit inventory ticks until actual shutdown.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            if (event.getServer() != level.getServer()) return;
            if (!player.isRemoved()) {
                final var tablet = player.getMainHandItem();
                if (tablet.is(ModItems.TABLET.get())) ModItems.TABLET.get().charge(tablet, 1000, false);
                player.doTick();
            }
        });
        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertTrue(marker(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM)).equals(prepare ? "waiting for process restart" : "resumed after process restart"),
                    "Probe continuation not ready: " + computer.machine().lastError());
                if (prepare) {
                    final var tablet = TabletRuntimeRegistry.get(player.getMainHandItem(), player);
                    tablet.save();
                    helper.assertTrue(marker(ModItems.TABLET.get().getComponent(tablet.stack(), 3)).equals("tablet checkpoint"), "Tablet checkpoint not ready");
                }
            })
            .thenExecute(() -> {
                final var data = new CompoundTag();
                data.putLong("pid", ProcessHandle.current().pid());
                data.putString("phase", prepare ? "prepared" : "verified");
                final var paper = new ItemStack(Items.PAPER);
                paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                record.setItem(0, paper);
                record.setChanged();
                NeoOpenComputers.LOGGER.info("Process restart probe {} in PID {}; normal shutdown will save world and player", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }

    private static ServerPlayer connectPlayer(GameTestHelper helper) {
        final var cookie = CommonListenerCookie.createInitial(new GameProfile(PLAYER_ID, "oc-restart-probe"), false);
        final var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        final var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setNoGravity(true);
        player.teleportTo(8.5, 82, 8.5);
        return player;
    }

    private static ItemStack cpu() {
        final var stack = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(stack)).setArchitecture(stack, NativeLuaArchitecture.class);
        return stack;
    }

    private static String marker(ItemStack stack) {
        return new String(ItemDriverData.dataTag(stack).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
