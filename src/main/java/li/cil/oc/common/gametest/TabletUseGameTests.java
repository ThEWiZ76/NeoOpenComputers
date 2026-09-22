package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletUseGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void shortUseOpensRunningTabletTerminal(GameTestHelper helper) { useTerminal(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeTabletUseAndPlayerSave(GameTestHelper helper) { useTerminal(helper, true); }

    private static void useTerminal(GameTestHelper helper, boolean nativeLua) {
        final var player = helper.makeMockServerPlayerInLevel();
        TabletEditorGameTests.enableMenuChannel(player);
        player.setNoGravity(true);
        final var item = ModItems.TABLET.get();
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        if (nativeLua) ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()),
            RobotMovementPersistenceGameTests.eeprom("""
                local gpu = component.proxy(component.list('gpu')())
                local screen = component.list('screen')()
                assert(gpu.bind(screen))
                gpu.set(1, 1, 'tablet online')
                repeat
                  local signal, address, char = computer.pullSignal()
                until signal == 'key_down' and char == 120
                local eeprom = component.proxy(component.list('eeprom')())
                eeprom.setData('input accepted')
                gpu.set(1, 1, 'input accepted')
                local scan
                repeat local event, data = computer.pullSignal(); if event == 'tablet_use' then scan = data end until scan
                assert(scan.signText == 'tablet' .. string.char(10) .. 'scan' .. string.char(10,10), 'scan payload lost')
                eeprom.setData('scan accepted')
                gpu.set(1, 1, 'scan accepted')
                while true do computer.pullSignal() end
                """), new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), new ItemStack(ModItems.KEYBOARD.get()), new ItemStack(ModItems.SIGN_UPGRADE.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // Embedded connections do not call doTick, which drives inventory and held-item use.
        for (int tick = 1; tick < 400; tick++) helper.runAtTickTime(tick, player::doTick);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "Tablet did not begin use");
        player.releaseUsingItem();
        helper.assertTrue(player.containerMenu instanceof TerminalMenu, "Tablet did not open terminal");
        final TerminalMenu menu = (TerminalMenu) player.containerMenu;
        final var runtime = TabletRuntimeRegistry.get(stack, player);
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(menu.itemScreen() != null
                && menu.itemScreen().terminalSnapshot().line(0).startsWith("tablet online"), "Tablet did not boot through inventory ticks: " + runtime.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(menu.acceptsInput(player), "Owner cannot type into tablet");
                helper.assertTrue(!menu.stillValid(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)), "Other player can use tablet menu");
                menu.itemScreen().keyDown('x', 45, player);
            })
            .thenWaitUntil(() -> helper.assertTrue(menu.itemScreen().terminalSnapshot().line(0).startsWith("input accepted"), "Tablet did not receive keyboard input"))
            .thenExecute(() -> {
                helper.assertTrue(item.isRunning(stack), "Tablet item running state stale");
                final var savedPlayer = player.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                final var inventory = savedPlayer.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND);
                final ItemStack saved = ItemStack.parseOptional(helper.getLevel().registryAccess(), inventory.getCompound(0));
                final String marker = new String(ItemDriverData.dataTag(item.getComponent(saved, 3)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
                helper.assertTrue(marker.equals("input accepted"), "Player save captured stale tablet components");
                if (nativeLua) {
                    final var runtimeData = saved.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag()
                        .getCompound("oc:tablet").getCompound("runtime").getCompound("machine");
                    helper.assertTrue(runtimeData.getCompound("architecture").contains("snapshot"), "Player save omitted native execution state");
                }
                player.closeContainer();
                final var signPos = new net.minecraft.core.BlockPos(1, 1, 1);
                helper.setBlock(signPos, net.minecraft.world.level.block.Blocks.OAK_SIGN);
                final net.minecraft.world.level.block.entity.SignBlockEntity sign = helper.getBlockEntity(signPos);
                sign.setText(sign.getFrontText().setMessage(0, net.minecraft.network.chat.Component.literal("tablet"))
                    .setMessage(1, net.minecraft.network.chat.Component.literal("scan")), true);
                final var target = helper.absolutePos(signPos);
                player.teleportTo(target.getX() + 2.5, target.getY(), target.getZ() + 0.5);
                final var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target), net.minecraft.core.Direction.NORTH, target, false);
                item.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            })
            .thenIdle(10)
            .thenExecute(player::releaseUsingItem)
            .thenWaitUntil(() -> helper.assertTrue(runtime.screen().terminalSnapshot().line(0).startsWith("scan accepted"), "Tablet scan failed: " + runtime.machine().lastError()))
            .thenExecute(() -> {
                if (nativeLua) {
                    helper.assertTrue(player.drop(false), "Tablet was not dropped");
                    helper.assertTrue(runtime.isClosed(), "Dropped tablet retained live runtime");
                    final var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        player.getBoundingBox().inflate(4), entity -> entity.getItem().is(ModItems.TABLET.get()));
                    helper.assertTrue(drops.size() == 1, "Expected one dropped tablet");
                    final ItemStack dropped = drops.getFirst().getItem();
                    final String marker = new String(ItemDriverData.dataTag(item.getComponent(dropped, 3)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
                    helper.assertTrue(marker.equals("scan accepted") && !item.isRunning(dropped), "Dropped tablet lost latest state or remained running");
                    drops.getFirst().discard();
                } else {
                    player.setShiftKeyDown(true);
                    item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    player.releaseUsingItem();
                    helper.assertTrue(!item.isRunning(stack), "Sneak-use did not stop tablet");
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.assertTrue(runtime.isClosed(), "Logout retained tablet runtime");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void tabletMovesWithoutSharingRuntimeWithCopies(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var item = ModItems.TABLET.get();
        final ItemStack first = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, first);
        final var original = TabletRuntimeRegistry.get(first, player);
        final ItemStack moved = first.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, moved);
        helper.assertTrue(TabletRuntimeRegistry.get(moved, player) == original, "Moving item replaced live runtime");
        final ItemStack copy = moved.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, copy);
        final var duplicate = TabletRuntimeRegistry.get(copy, player);
        helper.assertTrue(duplicate != original && duplicate.machine() != original.machine(), "Copy shares original machine");
        helper.assertTrue(original.stack() == moved, "Copy stole original runtime binding");
        final var menu = new li.cil.oc.common.menu.TabletTerminalMenu(37, player.getInventory(), original);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player), "Dropped tablet menu is still usable");
        helper.startSequence().thenIdle(201).thenExecute(() -> {
            helper.assertTrue(original.isClosed() && duplicate.isClosed(), "Idle tablet runtimes not evicted");
            helper.getLevel().getServer().getPlayerList().remove(player);
        }).thenSucceed();
    }
}
