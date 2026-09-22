package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletOpenOsGameTests {
    @GameTest(template = "empty", timeoutTicks = 2200)
    public static void tabletOpenOsRunsTypedCommand(GameTestHelper helper) { shell(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 2200)
    public static void nativeTabletOpenOsResumesShellAfterDimensionChange(GameTestHelper helper) { shell(helper, true); }

    private static void shell(GameTestHelper helper, boolean nativeLua) {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setNoGravity(true);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        if (nativeLua) ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()), API.items.get("luabios").createItemStack(1),
            new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), new ItemStack(ModItems.KEYBOARD.get()),
            API.items.get("openos").createItemStack(1));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int tick = 1; tick < 2200; tick++) helper.runAtTickTime(tick, () -> {
            if (!player.isRemoved()) {
                // Keep this long boot/continuation fixture powered through the normal item charger API.
                item.charge(stack, 1000, false);
                player.doTick();
            }
        });
        openTerminal(helper, player);
        final TabletRuntime[] runtime = {TabletRuntimeRegistry.get(stack, player)};
        final String firstCommand = nativeLua ? "set ocresume=731" : "echo ocok";
        final var sequence = helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(text(runtime[0]).contains("/home # "), "Tablet OpenOS did not boot: " + runtime[0].machine().lastError() + "\n" + text(runtime[0])))
            .thenExecute(() -> type(player, runtime[0], firstCommand))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime[0]).contains("/home # " + firstCommand), "Tablet did not echo typed command"))
            .thenExecute(() -> key(player, runtime[0], '\r', 0x1C))
            .thenWaitUntil(() -> helper.assertTrue(text(runtime[0]).split("/home # ", -1).length >= 3, "Tablet shell did not finish command: " + runtime[0].machine().lastError() + "\n" + text(runtime[0])));
        if (nativeLua) {
            sequence.thenExecute(() -> {
                    final TabletRuntime old = runtime[0];
                    final var componentAddresses = java.util.Map.copyOf(old.machine().components());
                    final TerminalMenu oldMenu = (TerminalMenu) player.containerMenu;
                    final var destination = helper.getLevel().getServer().getLevel(Level.NETHER);
                    helper.assertTrue(destination != null, "Nether unavailable");
                    helper.assertTrue(player.changeDimension(new DimensionTransition(destination, new Vec3(0.5, 200, 0.5), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING)) == player, "Player travel failed");
                    player.doTick();
                    helper.assertTrue(old.isClosed() && !old.machine().architecture().isInitialized(), "Old tablet VM survived dimension change");
                    helper.assertTrue(!oldMenu.stillValid(player), "Old terminal remained valid in another dimension");
                    player.closeContainer();
                    openTerminal(helper, player);
                    runtime[0] = TabletRuntimeRegistry.get(stack, player);
                    helper.assertTrue(runtime[0] != old && runtime[0].machine().isRunning(), "OpenOS tablet not restored");
                    helper.assertTrue(runtime[0].machine().components().equals(componentAddresses), "OpenOS component identities changed during travel");
                })
                .thenExecute(() -> type(player, runtime[0], "echo $ocresume"))
                .thenWaitUntil(() -> helper.assertTrue(text(runtime[0]).contains("/home # echo $ocresume"), "Restored shell did not receive input: " + runtime[0].machine().lastError()))
                .thenExecute(() -> key(player, runtime[0], '\r', 0x1C));
        }
        sequence.thenWaitUntil(() -> helper.assertTrue(java.util.regex.Pattern.compile(nativeLua ? "(?m)^731\\s*$" : "(?m)^ocok\\s*$").matcher(text(runtime[0])).find(),
                "Tablet shell output missing: " + runtime[0].machine().lastError() + "\n" + text(runtime[0])))
            .thenExecute(() -> {
                player.closeContainer();
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.assertTrue(runtime[0].isClosed(), "Logout retained OpenOS tablet VM");
            }).thenSucceed();
    }

    private static void openTerminal(GameTestHelper helper, ServerPlayer player) {
        ModItems.TABLET.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.releaseUsingItem();
        helper.assertTrue(player.containerMenu instanceof TerminalMenu menu && menu.acceptsInput(player), "Tablet terminal unavailable");
    }

    private static void type(ServerPlayer player, TabletRuntime runtime, String text) {
        for (char c : text.toCharArray()) key(player, runtime, c, NeoOpenComputersGameTests.keyCode(c));
    }

    private static void key(ServerPlayer player, TabletRuntime runtime, char character, int code) {
        runtime.screen().keyDown(character, code, player);
        runtime.screen().keyUp(character, code, player);
    }

    private static String text(TabletRuntime runtime) {
        final var snapshot = runtime.screen().terminalSnapshot();
        final StringBuilder text = new StringBuilder();
        for (int row = 0; row < runtime.screen().getHeight(); row++) text.append(snapshot.line(row)).append('\n');
        return text.toString();
    }
}
