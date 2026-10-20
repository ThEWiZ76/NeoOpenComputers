package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.*;
import li.cil.oc.common.menu.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.BiFunction;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class BlockMenuAccessGameTests {
    private static void verify(GameTestHelper helper, Block block,
                               BiFunction<Inventory, BlockEntity, AbstractContainerMenu> menuFactory) {
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var state = block.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        final var host = level.getBlockEntity(pos);
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var menu = menuFactory.apply(player.getInventory(), host);
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(menu.stillValid(player), "Current nearby host is unusable");
            player.setPos(pos.getX() + 8.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(menu.stillValid(player), "Exact eight-block menu boundary is rejected");
            player.setPos(pos.getX() + 8.625, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(!menu.stillValid(player), "Menu remains usable beyond eight blocks");
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            final var nether = level.getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Nether is unavailable");
            player.setServerLevel(nether);
            helper.assertTrue(!menu.stillValid(player), "Menu accepts a player in another dimension");
            player.setServerLevel(level);
            helper.assertTrue(!menu.stillValid(null), "Server menu accepts a missing player");

            // Keep the stale object alive to distinguish identity checks from removal checks.
            level.removeBlockEntity(pos);
            host.clearRemoved();
            level.setBlockEntity(((EntityBlock) block).newBlockEntity(pos, state));
            helper.assertTrue(!menu.stillValid(player), "Menu accepts an old host after replacement");
            host.setRemoved();
        } finally {
            player.setServerLevel(level);
            level.getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "ae2_export") public static void adapterMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.ADAPTER.get(), (i, b) -> new AdapterMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void assemblerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.ASSEMBLER.get(), (i, b) -> new AssemblerMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void chargerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.CHARGER.get(), (i, b) -> new ChargerMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void computerCaseMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.COMPUTER_CASE_TIER1.get(), (i, b) -> new ComputerCaseMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void disassemblerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.DISASSEMBLER.get(), (i, b) -> new DisassemblerMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void diskDriveMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.DISK_DRIVE.get(), (i, b) -> new DiskDriveMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void printerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.PRINTER.get(), (i, b) -> new PrinterMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void rackMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.RACK.get(), (i, b) -> new RackMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void relayMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.RELAY.get(), (i, b) -> new RelayMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void raidMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.RAID.get(), (i, b) -> new RaidMenu(1, i, (Container) b));
    }
    @GameTest(template = "ae2_export") public static void microcontrollerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.MICROCONTROLLER_TIER1.get(), (i, b) -> new MicrocontrollerMenu(1, i, (MicrocontrollerBlockEntity) b));
    }
    @GameTest(template = "ae2_export") public static void robotMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.ROBOT.get(), (i, b) -> new RobotMenu(1, i, (RobotBlockEntity) b));
    }
    @GameTest(template = "ae2_export") public static void waypointMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.WAYPOINT.get(), (i, b) -> new WaypointMenu(1, i, (WaypointBlockEntity) b));
    }

    @GameTest(template = "ae2_export") public static void rackServerMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.RACK.get(), (i, b) -> {
            final var rack = (RackBlockEntity) b;
            rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
            return new ServerRackMenu(1, i, (Container) rack.getMountable(0));
        });
    }
    @GameTest(template = "ae2_export") public static void rackDiskDriveMenuAccess(GameTestHelper h) {
        verify(h, ModBlocks.RACK.get(), (i, b) -> {
            final var rack = (RackBlockEntity) b;
            rack.setItem(0, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get()));
            return new DiskDriveMenu(1, i, (Container) rack.getMountable(0));
        });
    }

    @GameTest(template = "ae2_export")
    public static void blockInventoriesRejectUnloadedChunksWithoutLoading(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = new BlockPos(24_000_000, 80, 24_000_000);
        helper.assertTrue(!level.hasChunkAt(pos), "Unloaded menu fixture chunk is already loaded");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        helper.assertTrue(!level.hasChunkAt(pos), "Moving the unregistered player loaded the fixture chunk");
        final Block[] blocks = {ModBlocks.ADAPTER.get(), ModBlocks.ASSEMBLER.get(), ModBlocks.CHARGER.get(),
            ModBlocks.COMPUTER_CASE_TIER1.get(), ModBlocks.DISASSEMBLER.get(), ModBlocks.DISK_DRIVE.get(),
            ModBlocks.PRINTER.get(), ModBlocks.RACK.get(), ModBlocks.RELAY.get(), ModBlocks.RAID.get(),
            ModBlocks.MICROCONTROLLER_TIER1.get(), ModBlocks.ROBOT.get(), ModBlocks.WAYPOINT.get()};
        for (var block : blocks) {
                final var host = ((EntityBlock) block).newBlockEntity(pos, block.defaultBlockState());
                host.setLevel(level);
                try {
                    final boolean valid = host instanceof Container inventory ? inventory.stillValid(player)
                        : new WaypointMenu(1, player.getInventory(), (WaypointBlockEntity) host).stillValid(player);
                    helper.assertTrue(!valid, "Menu accepts an unloaded host: " + block);
                    helper.assertTrue(!level.hasChunkAt(pos), "Menu lookup loaded an absent chunk: " + block);
                } finally { host.setRemoved(); }
        }
        helper.succeed();
    }
}
