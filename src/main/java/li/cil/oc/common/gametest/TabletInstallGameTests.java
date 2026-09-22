package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import li.cil.oc.common.menu.TabletMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc.common.gametest.TabletOpenOsGameTests.*;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletInstallGameTests {
    @GameTest(template = "empty", timeoutTicks = 6000)
    public static void nativeTabletInstallsOpenOsAndBootsWithoutFloppy(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        TabletEditorGameTests.enableMenuChannel(player);
        player.setNoGravity(true);
        final var cpu = new ItemStack(ModItems.CPU_TIER2.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            new ItemStack(ModItems.DISK_DRIVE.get()), cpu, new ItemStack(ModItems.MEMORY_TIER2.get()),
            new ItemStack(ModItems.MEMORY_TIER2.get()), API.items.get("luabios").createItemStack(1),
            new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), new ItemStack(ModItems.KEYBOARD.get()), new ItemStack(ModItems.HDD_TIER1.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.getInventory().setItem(9, API.items.get("openos").createItemStack(1));
        player.setShiftKeyDown(true);
        item.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.releaseUsingItem();
        helper.assertTrue(player.containerMenu instanceof TabletMenu, "Tablet editor did not open for install media");
        final var editor = (TabletMenu) player.containerMenu;
        editor.clicked(editor.findSlot(player.getInventory(), 9).orElseThrow(), 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(item.getComponent(stack, 31).is(ModItems.FLOPPY.get()), "Editor did not install OpenOS floppy");
        player.closeContainer();
        player.setShiftKeyDown(false);
        openTerminal(helper, player);
        final var runtime = TabletRuntimeRegistry.get(stack, player);
        for (int tick = 1; tick < 6000; tick++) helper.runAtTickTime(tick, () -> {
            if (!player.isRemoved()) {
                item.charge(stack, 1000, false);
                player.doTick();
            }
        });
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("/home # "), "Install media did not boot: " + runtime.machine().lastError() + "\n" + text(runtime)))
            .thenExecute(() -> runtime.screen().clipboard("install --noreboot", player))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("/home # install --noreboot"), "Install command not entered"))
            .thenExecute(() -> key(player, runtime, '\r', 0x1C))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("Install OpenOS? [Y/n]"), "Installer did not ask for confirmation: " + runtime.machine().lastError() + "\n" + text(runtime)))
            .thenExecute(() -> key(player, runtime, '\r', 0x1C))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("Installation complete!") && text(runtime).contains("Returning to shell."), "OpenOS installation failed: " + runtime.machine().lastError() + "\n" + text(runtime)))
            .thenExecute(() -> {
                player.closeContainer();
                player.setShiftKeyDown(true);
                item.use(player.level(), player, InteractionHand.MAIN_HAND);
                player.releaseUsingItem();
                helper.assertTrue(player.containerMenu instanceof TabletMenu, "Editor unavailable after install");
                final var menu = (TabletMenu) player.containerMenu;
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(item.getComponent(stack, 31).isEmpty(), "Install floppy remained in tablet");
                helper.assertTrue(runtime.machine().components().values().stream().filter("filesystem"::equals).count() == 2, "Removed install filesystem still connected");
                player.closeContainer();
                player.setShiftKeyDown(false);
                openTerminal(helper, player);
            })
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("/home # ") && !text(runtime).contains("readonly"), "Installed HDD did not boot: " + runtime.machine().lastError() + "\n" + text(runtime)))
            .thenExecute(() -> runtime.screen().clipboard("echo installed > /home/install-proof", player))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("echo installed > /home/install-proof"), "Write command not entered"))
            .thenExecute(() -> key(player, runtime, '\r', 0x1C))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).split("/home # ", -1).length >= 3, "Installed shell did not complete write"))
            .thenExecute(() -> runtime.screen().clipboard("cat /home/install-proof", player))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime).contains("/home # cat /home/install-proof"), "Readback command not entered"))
            .thenExecute(() -> key(player, runtime, '\r', 0x1C))
            .thenWaitUntil(() -> helper.assertTrue(java.util.regex.Pattern.compile("(?m)^/home # cat /home/install-proof[^\\S\\n]*\\ninstalled[^\\S\\n]*$").matcher(text(runtime)).find(), "Installed writable system failed readback: " + runtime.machine().lastError() + "\n" + text(runtime)))
            .thenExecute(() -> {
                player.closeContainer();
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.assertTrue(runtime.isClosed(), "Installed tablet VM survived logout");
            }).thenSucceed();
    }
}
