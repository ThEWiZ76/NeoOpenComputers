package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletContainerGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void tabletShiftClickSavesLatestStateToChest(GameTestHelper helper) { transfer(helper, true); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void tabletCursorTransferRetainsVmUntilStored(GameTestHelper helper) { transfer(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void fullChestDoesNotStopCarriedTablet(GameTestHelper helper) { transfer(helper, true, true); }

    private static void transfer(GameTestHelper helper, boolean shiftClick) {
        transfer(helper, shiftClick, false);
    }

    private static void transfer(GameTestHelper helper, boolean shiftClick, boolean full) {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setNoGravity(true);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                eeprom.setData('newest unsaved data')
                while true do computer.pullSignal() end
                """));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final TabletRuntime runtime = TabletRuntimeRegistry.get(stack, player);
        helper.assertTrue(runtime.start(), "Tablet did not start");
        for (int tick = 1; tick < 400; tick++) helper.runAtTickTime(tick, player::doTick);
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(pos);
        if (full) for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
        player.teleportTo(chest.getBlockPos().getX() + 0.5, chest.getBlockPos().getY() + 1, chest.getBlockPos().getZ() + 0.5);
        player.openMenu(chest);
        final ChestMenu menu = (ChestMenu) player.containerMenu;
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(liveMarker(runtime).equals("newest unsaved data"), "Tablet program failed: " + runtime.machine().lastError()))
            .thenExecute(() -> {
                final int source = menu.findSlot(player.getInventory(), player.getInventory().selected).orElseThrow();
                menu.clicked(source, 0, shiftClick ? ClickType.QUICK_MOVE : ClickType.PICKUP, player);
                if (full) {
                    helper.assertTrue(!runtime.isClosed() && runtime.machine().isRunning() && player.getMainHandItem() == stack,
                        "Failed transfer stopped or replaced carried tablet");
                    player.closeContainer();
                    helper.getLevel().getServer().getPlayerList().remove(player);
                    return;
                }
                if (!shiftClick) {
                    helper.assertTrue(!runtime.isClosed() && runtime.stack() == menu.getCarried(), "Cursor transfer lost running VM");
                    helper.assertTrue(TabletRuntimeRegistry.get(menu.getCarried(), player) == runtime, "Cursor created another runtime");
                    menu.clicked(0, 0, ClickType.PICKUP, player);
                }
                helper.assertTrue(runtime.isClosed() && !runtime.machine().architecture().isInitialized(), "Stored tablet retained live VM");
                final ItemStack stored = chest.getItem(0);
                helper.assertTrue(stored.is(ModItems.TABLET.get()) && !item.isRunning(stored), "Chest did not receive stopped tablet");
                helper.assertTrue(marker(item.getComponent(stored, 3)).equals("newest unsaved data"), "Chest received stale tablet components");
                final var saved = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
                final var restored = (ChestBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.assertTrue(marker(item.getComponent(restored.getItem(0), 3)).equals("newest unsaved data"), "Chest serialization lost tablet data");
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                ItemStack returned = ItemStack.EMPTY;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (player.getInventory().getItem(slot).is(ModItems.TABLET.get())) returned = player.getInventory().getItem(slot);
                }
                helper.assertTrue(!returned.isEmpty() && chest.getItem(0).isEmpty(), "Tablet did not return to inventory");
                final TabletRuntime fresh = TabletRuntimeRegistry.get(returned, player);
                helper.assertTrue(fresh != runtime && !fresh.machine().isRunning(), "Stored tablet resumed an evicted VM");
                helper.assertTrue(marker(item.getComponent(returned, 3)).equals("newest unsaved data"), "Retrieval lost tablet data");
                player.closeContainer();
                helper.getLevel().getServer().getPlayerList().remove(player);
            }).thenSucceed();
    }

    private static String liveMarker(TabletRuntime runtime) {
        for (ItemStack stack : runtime.internalComponents()) {
            final String value = marker(stack);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String marker(ItemStack stack) {
        return new String(ItemDriverData.dataTag(stack).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
