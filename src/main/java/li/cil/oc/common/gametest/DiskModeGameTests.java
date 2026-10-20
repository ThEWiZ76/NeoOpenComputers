package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.item.HardDiskDriveItem;
import li.cil.oc.common.menu.DriveMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DiskModeGameTests {
    @GameTest(template = "empty")
    public static void ordinaryFloppyOpensItsHeldDriveMenuAndHonorsHandOwnership(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(ModItems.FLOPPY.get());
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(player.containerMenu instanceof DriveMenu, "Ordinary floppy has no drive mode/lock interaction");
        final var menu = (DriveMenu) player.containerMenu;
        helper.assertTrue(menu.stillValid(player) && menu.clickMenuButton(player, DriveMenu.BUTTON_UNMANAGED) && menu.isUnmanaged(), "Floppy drive menu does not apply raw mode");
        helper.assertTrue(menu.clickMenuButton(player, DriveMenu.BUTTON_LOCK) && menu.isLocked(), "Floppy drive menu cannot lock the raw disk");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player, DriveMenu.BUTTON_MANAGED), "Stale held-disk menu changed a removed floppy");
        player.containerMenu = player.inventoryMenu;
        player.setItemInHand(InteractionHand.MAIN_HAND, stack); player.setShiftKeyDown(true);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Sneak-use unexpectedly opened the floppy mode menu");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void floppyRawModeUsesItsOwnCapacityAndPersistsLockedSectorData(GameTestHelper helper) throws Exception {
        final var stack = new ItemStack(ModItems.FLOPPY.get());
        HardDiskDriveItem.setUnmanaged(stack, true);
        final var first = environment(stack);
        try {
            final var raw = (Component) first.node();
            helper.assertTrue(raw.name().equals("drive"), "Unmanaged floppy still exposes a managed filesystem");
            helper.assertTrue(((Number) raw.invoke("getCapacity", null)[0]).longValue() == ModSettings.floppySize() * 1024L && ((Number) raw.invoke("getPlatterCount", null)[0]).intValue() == 1, "Raw floppy uses HDD size/platter settings");
            raw.invoke("writeByte", null, 1, 255); raw.invoke("setLabel", null, "Raw floppy"); first.save(new CompoundTag());
        } finally { first.node().remove(); }
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        final var restored = environment(stack);
        try {
            final var raw = (Component) restored.node();
            helper.assertTrue(((Number) raw.invoke("readByte", null, 1)[0]).intValue() == -1 && "Raw floppy".equals(raw.invoke("getLabel", null)[0]), "Raw floppy bytes/label did not persist");
            denied(helper, () -> raw.invoke("writeByte", null, 1, 42));
            denied(helper, () -> raw.invoke("setLabel", null, "Changed"));
            restored.save(new CompoundTag());
            helper.assertTrue(HardDiskDriveItem.lockInfo(stack).equals("Fixture owner") && HardDiskDriveItem.isUnmanaged(stack), "Raw floppy save lost mode or lock");
        } finally { restored.node().remove(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void floppyManagedLockPreservesBinaryContentAndRejectsWrites(GameTestHelper helper) throws Exception {
        final var stack = prepared(new ItemStack(ModItems.FLOPPY.get()));
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        final var restored = environment(stack);
        try {
            final var fs = (Component) restored.node();
            helper.assertTrue(Boolean.TRUE.equals(fs.invoke("isReadOnly", null)[0]), "Locked managed floppy is writable");
            denied(helper, () -> fs.invoke("open", null, "probe.bin", "wb"));
            denied(helper, () -> fs.invoke("setLabel", null, "Changed"));
            final var h = fs.invoke("open", null, "probe.bin", "rb")[0];
            helper.assertTrue(Arrays.equals(new byte[]{97, 0, (byte) 128, (byte) 255}, (byte[]) fs.invoke("read", null, h, 16)[0]), "Floppy lock damaged binary content");
            fs.invoke("close", null, h); restored.save(new CompoundTag());
            helper.assertTrue(HardDiskDriveItem.lockInfo(stack).equals("Fixture owner"), "Managed floppy save discarded its lock");
        } finally { restored.node().remove(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realModeChangesFormatStorageAndReplaceIdentityButKeepLabels(GameTestHelper helper) throws Exception {
        for (final var item : List.of(ModItems.HDD_TIER1.get(), ModItems.FLOPPY.get())) {
            final var stack = prepared(new ItemStack(item));
            final String oldAddress = HardDiskDriveItem.stackDataTag(stack).getCompound("node").getString("address");
            HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
            HardDiskDriveItem.setUnmanaged(stack, true);
            helper.assertTrue(!HardDiskDriveItem.isLocked(stack), "Actual mode change did not clear the lock");
            final var raw = environment(stack);
            try {
                final var drive = (Component) raw.node();
                helper.assertTrue(!oldAddress.equals(drive.address()), "Mode change reused the old filesystem identity");
                helper.assertTrue(drive.name().equals("drive") && ((Number) drive.invoke("readByte", null, 1)[0]).intValue() == 0 && "Keep label".equals(drive.invoke("getLabel", null)[0]), "Raw mode did not start empty with the old label");
                drive.invoke("writeByte", null, 1, 255); raw.save(new CompoundTag());
            } finally { raw.node().remove(); }
            HardDiskDriveItem.setUnmanaged(stack, false);
            final var managed = environment(stack);
            try {
                final var fs = (Component) managed.node();
                helper.assertTrue(fs.name().equals("filesystem") && Boolean.FALSE.equals(fs.invoke("exists", null, "probe.bin")[0]) && "Keep label".equals(fs.invoke("getLabel", null)[0]), "Switching back resurrected old files or lost the label");
                managed.save(new CompoundTag());
            } finally { managed.node().remove(); }
            HardDiskDriveItem.setUnmanaged(stack, true);
            final var emptyRaw = environment(stack);
            try { helper.assertTrue(((Number) ((Component) emptyRaw.node()).invoke("readByte", null, 1)[0]).intValue() == 0, "Raw data resurrected after another mode change"); }
            finally { emptyRaw.node().remove(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repeatedModeSelectionAndLootMenusDoNotDestroyData(GameTestHelper helper) throws Exception {
        final var stack = prepared(new ItemStack(ModItems.HDD_TIER1.get()));
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        HardDiskDriveItem.stackDataTag(stack).putBoolean("oc:unmanaged", false);
        final var before = HardDiskDriveItem.stackDataTag(stack).copy();
        HardDiskDriveItem.setUnmanaged(stack, false);
        helper.assertTrue(before.equals(HardDiskDriveItem.stackDataTag(stack)), "Repeated selection of the current mode altered driver data");
        for (final var loot : ModLootDisks.bundledStacksForCycling()) {
            final var player = helper.makeMockServerPlayerInLevel(); player.setItemInHand(InteractionHand.MAIN_HAND, loot);
            final var menu = new DriveMenu(51, player.getInventory(), loot);
            helper.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player, DriveMenu.BUTTON_UNMANAGED), "Loot disk accepted destructive mode changes");
            loot.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!(player.containerMenu instanceof DriveMenu), "Loot disk opened a writable-disk mode menu");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void modeAndLockActionsDoNotMutateInventorySnapshots(GameTestHelper helper) {
        final var stack = new ItemStack(ModItems.HDD_TIER1.get());
        HardDiskDriveItem.stackDataTag(stack).putBoolean("oc:unmanaged", false);
        final var old = stack.copy();
        HardDiskDriveItem.setUnmanaged(stack, true);
        helper.assertTrue(!HardDiskDriveItem.isUnmanaged(old) && HardDiskDriveItem.isUnmanaged(stack), "Mode mutation contaminated an existing inventory snapshot");
        final var beforeLock = stack.copy();
        HardDiskDriveItem.lock(stack, null);
        helper.assertTrue(!HardDiskDriveItem.isLocked(beforeLock) && HardDiskDriveItem.isLocked(stack), "Lock mutation contaminated an existing inventory snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyFloppyModeAndLockRemainVisibleToTheMenuAndDriver(GameTestHelper helper) throws Exception {
        final var stack = new ItemStack(ModItems.FLOPPY.get());
        final var data = new CompoundTag(); data.putBoolean("oc:unmanaged", true); data.putString("oc:lock", "Legacy owner");
        data.putString("oc:fs.label", "Legacy raw"); data.putByteArray("data", new byte[]{(byte) 255});
        final var root = new CompoundTag(); root.put("oc:floppy", data);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
        helper.assertTrue(HardDiskDriveItem.isUnmanaged(stack) && HardDiskDriveItem.lockInfo(stack).equals("Legacy owner"), "Drive state helpers ignored legacy floppy configuration");
        final var env = environment(stack);
        try {
            final var raw = (Component) env.node();
            helper.assertTrue(raw.name().equals("drive") && ((Number) raw.invoke("readByte", null, 1)[0]).intValue() == -1, "Legacy raw floppy data was not loaded");
            denied(helper, () -> raw.invoke("writeByte", null, 1, 42)); env.save(new CompoundTag());
            helper.assertTrue(HardDiskDriveItem.lockInfo(stack).equals("Legacy owner"), "Legacy floppy lock did not survive save");
        } finally { env.node().remove(); }
        helper.succeed();
    }

    private static ItemStack prepared(ItemStack stack) throws Exception {
        final var env = environment(stack);
        try {
            final var fs = (Component) env.node(); final var h = fs.invoke("open", null, "probe.bin", "wb")[0];
            fs.invoke("write", null, h, new byte[]{97, 0, (byte) 128, (byte) 255}); fs.invoke("close", null, h);
            fs.invoke("setLabel", null, "Keep label"); env.save(new CompoundTag());
        } finally { env.node().remove(); }
        return stack;
    }

    static ItemStack rawRestartFloppy() throws Exception {
        final var stack = new ItemStack(ModItems.FLOPPY.get()); HardDiskDriveItem.setUnmanaged(stack, true);
        final var env = environment(stack);
        try {
            final var raw = (Component) env.node(); raw.invoke("writeByte", null, 1, 255);
            raw.invoke("setLabel", null, "Raw restart"); env.save(new CompoundTag());
        } finally { env.node().remove(); }
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        return stack;
    }

    private static ManagedEnvironment environment(ItemStack stack) {
        final var env = Driver.driverFor(stack).createEnvironment(stack, null);
        Network.joinNewNetwork(env.node());
        final var connector = (Connector) env.node(); connector.setLocalBufferSize(10000); connector.changeBuffer(10000);
        return env;
    }

    private static void denied(GameTestHelper helper, Attempt action) throws Exception {
        boolean denied = false;
        try { action.run(); } catch (Exception failure) {
            helper.assertTrue(failure.getMessage().contains("read only") || failure.getMessage().contains("read-only"), "Wrong mutation failure: " + failure);
            denied = true;
        }
        helper.assertTrue(denied, "Locked floppy accepted a mutation");
    }
    @FunctionalInterface private interface Attempt { void run() throws Exception; }
}
