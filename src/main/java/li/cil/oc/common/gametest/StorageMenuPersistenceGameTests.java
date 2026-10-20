package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.menu.DiskDriveMenu;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.menu.ServerRackMenu;
import li.cil.oc.common.menu.TabletMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class StorageMenuPersistenceGameTests {
    private static final byte[] BYTES = new byte[]{0, (byte) 128, (byte) 255};

    static void write(Component fs, Machine context) throws Exception {
        ((Connector) fs).changeBuffer(10000);
        final var h = fs.invoke("open", context, "menu.bin", "wb")[0]; fs.invoke("write", context, h, BYTES); fs.invoke("close", context, h);
    }
    static void verify(GameTestHelper helper, ItemStack stack, Machine context, String address) throws Exception {
        final var environment = Driver.driverFor(stack).createEnvironment(stack, context.host());
        helper.assertTrue(environment != null && environment.node() instanceof Component, "Transferred storage has no filesystem");
        final var fs = (Component) environment.node(); context.node().connect(fs); ((Connector) fs).changeBuffer(10000);
        try {
            helper.assertTrue(address.equals(fs.address()), "Menu transfer lost filesystem identity");
            helper.assertTrue(Boolean.TRUE.equals(fs.invoke("exists", context, "menu.bin")[0]), "Menu transfer lost latest file data");
            final var h = fs.invoke("open", context, "menu.bin", "rb")[0];
            helper.assertTrue(Arrays.equals((byte[]) fs.invoke("read", context, h, 3)[0], BYTES), "Menu transfer changed binary bytes"); fs.invoke("close", context, h);
        } finally { fs.remove(); }
    }
    static void move(GameTestHelper helper, AbstractContainerMenu menu, int source, Player player, Machine context, Component fs) throws Exception {
        write(fs, context); final String address = fs.address(); final var item = menu.getSlot(source).getItem().getItem();
        final var returned = menu.quickMoveStack(player, source);
        helper.assertTrue(returned.getCount() == 1 && menu.getSlot(source).getItem().isEmpty(), "Menu did not move exactly one source item");
        verify(helper, returned, context, address);
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final var destination = player.getInventory().getItem(slot);
            if (destination.is(item)) { count += destination.getCount(); verify(helper, destination, context, address); }
        }
        helper.assertTrue(count == 1, "Menu duplicated/lost the destination item");
    }

    @GameTest(template = "ae2_export")
    public static void blockDiskShiftMoveKeepsLatestBytes(GameTestHelper helper) throws Exception {
        final var hardware = DiskDriveEjectionGameTests.hardware(helper); final var player = helper.makeMockServerPlayerInLevel();
        try {
            hardware.drive().setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            move(helper, new DiskDriveMenu(61, player.getInventory(), hardware.drive()), 0, player, hardware.computer().machine(), DiskDriveEjectionGameTests.filesystem(helper, hardware));
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void rackDiskShiftMoveKeepsLatestBytes(GameTestHelper helper) throws Exception {
        final var hardware = DiskDriveEjectionGameTests.rackHardware(helper); final var player = helper.makeMockServerPlayerInLevel();
        try {
            hardware.drive().setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            move(helper, new DiskDriveMenu(62, player.getInventory(), hardware.drive()), 0, player, hardware.server().machine(), DiskDriveEjectionGameTests.rackFilesystem(helper, hardware.drive()));
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void internalStorageShiftMoveKeepsLatestBytes(GameTestHelper helper) throws Exception {
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3)); helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER3.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos); computer.onLoad();
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            computer.setItem(7, new ItemStack(ModItems.FLOPPY.get()));
            move(helper, new ComputerCaseMenu(63, player.getInventory(), computer), 7, player, computer.machine(), DiskDriveEjectionGameTests.internalFilesystem(helper, computer.machine(), 7));
            player.getInventory().clearContent();
            final var hardware = DiskDriveEjectionGameTests.rackHardware(helper); hardware.serverItems().setItem(6, new ItemStack(ModItems.HDD_TIER1.get()));
            move(helper, new ServerRackMenu(64, player.getInventory(), hardware.serverItems()), 6, player, hardware.server().machine(), DiskDriveEjectionGameTests.internalFilesystem(helper, hardware.server().machine(), 6));
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void tabletExpansionRemovalAndShiftMoveKeepBytes(GameTestHelper helper) throws Exception {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            for (int path = 0; path < 5; path++) {
                final var stack = ModItems.TABLET.get().assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), new ItemStack(ModItems.DISK_DRIVE.get()));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack); final var runtime = new TabletRuntime(stack, player);
                try {
                    final var menu = new TabletMenu(65, player.getInventory(), runtime); final Container inventory = menu.getSlot(0).container;
                    inventory.setItem(0, new ItemStack(ModItems.FLOPPY.get())); final var fs = DiskDriveEjectionGameTests.internalFilesystem(helper, runtime.machine(), 31);
                    if (path == 4) { move(helper, menu, 0, player, runtime.machine(), fs); continue; }
                    write(fs, runtime.machine()); final var address = fs.address(); final ItemStack removed;
                    if (path == 0) removed = inventory.removeItem(0, 1);
                    else if (path == 1) removed = inventory.removeItemNoUpdate(0);
                    else { removed = inventory.getItem(0); if (path == 2) inventory.setItem(0, ItemStack.EMPTY); else inventory.clearContent(); }
                    helper.assertTrue(ModItems.TABLET.get().getComponent(stack, 31).isEmpty(), "Removed expansion remains installed in tablet data");
                    verify(helper, removed, runtime.machine(), address);
                } finally { runtime.close(false); }
            }
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void removingWholeRackDriveDisposesChildFilesystem(GameTestHelper helper) {
        final var hardware = DiskDriveEjectionGameTests.rackHardware(helper); hardware.drive().setItem(0, new ItemStack(ModItems.FLOPPY.get()));
        final var fs = DiskDriveEjectionGameTests.rackFilesystem(helper, hardware.drive());
        final var removed = hardware.rack().removeItem(1, 1);
        helper.assertTrue(!removed.isEmpty() && fs.network() == null, "Removed rack module left an orphan filesystem network");
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void rackModuleShiftMoveKeepsNestedDiskData(GameTestHelper helper) throws Exception {
        final var hardware = DiskDriveEjectionGameTests.rackHardware(helper); final var player = helper.makeMockServerPlayerInLevel();
        try {
            hardware.drive().setItem(0, new ItemStack(ModItems.FLOPPY.get())); final var fs = DiskDriveEjectionGameTests.rackFilesystem(helper, hardware.drive());
            write(fs, hardware.server().machine()); final var address = fs.address();
            new RackMenu(66, player.getInventory(), hardware.rack()).quickMoveStack(player, 1);
            ItemStack transferred = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(ModItems.DISK_DRIVE_MOUNTABLE.get())) transferred = player.getInventory().getItem(i);
            helper.assertTrue(!transferred.isEmpty(), "Rack menu did not transfer module"); hardware.rack().setItem(1, transferred);
            final var drive = (Container) hardware.rack().getMountable(1);
            helper.assertTrue(!drive.getItem(0).isEmpty(), "Shift-moved rack module lost nested disk");
            verify(helper, drive.removeItem(0, 1), hardware.server().machine(), address);
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void robotRuntimeDiskShiftMoveKeepsLatestBytes(GameTestHelper helper) throws Exception {
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3)); helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (li.cil.oc.common.blockentity.RobotBlockEntity) helper.getLevel().getBlockEntity(pos);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.DISK_DRIVE.get()))); robot.onLoad();
        helper.assertTrue(robot.canPlaceItem(1, new ItemStack(ModItems.FLOPPY.get())), "Robot runtime slot rejected floppy"); robot.setItem(1, new ItemStack(ModItems.FLOPPY.get()));
        Component fs = null;
        for (var node : robot.machine().node().reachableNodes()) if (node instanceof Component value && value.name().equals("filesystem")
            && robot.componentSlot(value.address()) == li.cil.oc.common.blockentity.RobotBlockEntity.slotCount(2)) fs = value;
        helper.assertTrue(fs != null, "Robot runtime filesystem missing"); final var player = helper.makeMockServerPlayerInLevel();
        try { move(helper, new li.cil.oc.common.menu.RobotMenu(67, player.getInventory(), robot), 1, player, robot.machine(), fs); }
        finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
}
