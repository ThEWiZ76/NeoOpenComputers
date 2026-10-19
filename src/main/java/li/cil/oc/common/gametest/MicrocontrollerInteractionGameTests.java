package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.WrenchTools;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class MicrocontrollerInteractionGameTests {
    @GameTest(template = "empty")
    public static void microcontrollerAssemblyRejectsContainerMenuAndAutomationMutations(final GameTestHelper helper) {
        final var player = player(helper);
        final var host = fixture(helper, player);
        final var cpu = host.getItem(0).copy();
        helper.assertTrue(host.removeItem(0, 1).isEmpty(), "Immutable assembly allowed CPU extraction");
        helper.assertTrue(host.removeItemNoUpdate(0).isEmpty(), "Immutable assembly allowed no-update extraction");
        host.setItem(0, new ItemStack(ModItems.CPU_TIER2.get()));
        host.clearContent();
        helper.assertTrue(ItemStack.matches(cpu, host.getItem(0)) && !host.isEmpty(), "Container setter/clear changed assembled hardware");
        helper.assertTrue(!host.canPlaceItem(4, new ItemStack(ModItems.NETWORK_CARD.get())), "Assembly advertises mutable slots");
        final var inventory = new InvWrapper(host);
        helper.assertTrue(inventory.extractItem(0, 1, false).isEmpty(), "Automation extracted CPU");
        helper.assertTrue(inventory.insertItem(4, new ItemStack(ModItems.NETWORK_CARD.get()), false).getCount() == 1
            && host.getItem(4).isEmpty(), "Automation inserted hardware");
        final var menu = new MicrocontrollerMenu(73, player.getInventory(), host);
        helper.assertTrue(!menu.getSlot(0).mayPickup(player) && !menu.getSlot(4).mayPlace(new ItemStack(ModItems.NETWORK_CARD.get())),
            "Inspection menu advertises editable hardware");
        helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "Shift-click removed assembled CPU");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && ItemStack.matches(cpu, host.getItem(0)), "Menu pickup removed assembled CPU");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerClickTogglesPowerAndWrenchRemainsDelegated(final GameTestHelper helper) {
        final var player = player(helper);
        final var host = fixture(helper, player);
        ((Connector) host.machine().node()).changeBuffer(731);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        final var hit = hit(host);
        helper.assertTrue(host.getBlockState().useWithoutItem(helper.getLevel(), player, hit).consumesAction()
            && host.machine().isRunning(), "Ordinary activation did not start microcontroller");
        helper.assertTrue(host.getBlockState().useWithoutItem(helper.getLevel(), player, hit).consumesAction()
            && !host.machine().isRunning(), "Ordinary activation did not stop microcontroller");
        final var screwdriver = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .get(ResourceLocation.fromNamespaceAndPath("projectred_core", "screwdriver"));
        if (screwdriver != net.minecraft.world.item.Items.AIR) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(screwdriver));
            helper.assertTrue(WrenchTools.holdsApplicableWrench(player, host.getBlockPos()), "Real ProjectRed wrench fixture is not applicable");
            helper.assertTrue(host.getBlockState().useWithoutItem(helper.getLevel(), player, hit) == InteractionResult.PASS
                && !host.machine().isRunning(), "Microcontroller consumed wrench interaction");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void microcontrollerSneakEepromSwapPreservesNativeVmAndUsesNewChipOnReboot(final GameTestHelper helper) {
        final var player = player(helper);
        final var oldChip = RobotMovementPersistenceGameTests.eeprom("""
            local retained = 731
            local ee = component.proxy(component.list('eeprom')())
            local modem = component.proxy(component.list('modem')())
            modem.open(731)
            ee.setData('ready')
            repeat until computer.pullSignal() == 'after_swap'
            local replacement = component.proxy(component.list('eeprom')())
            assert(replacement.address ~= ee.address, 'old EEPROM stayed attached')
            assert(modem.isOpen(731), 'unrelated modem lifecycle reset')
            replacement.setData('continued:' .. retained)
            while true do computer.pullSignal() end
            """);
        final var item = MicrocontrollerItemRoundTripGameTests.assemble(helper, new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get()), oldChip);
        final var host = MicrocontrollerItemRoundTripGameTests.place(helper, player, item, new BlockPos(1, 0, 1));
        ((Connector) host.machine().node()).changeBuffer(1000);
        helper.assertTrue(host.toggleMachine(), "Native EEPROM swap fixture did not start");
        final var architecture = host.machine().architecture();
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(host).equals("ready"),
                "Old native program did not reach swap checkpoint: " + host.machine().lastError()))
            .thenExecute(() -> {
                final var replacement = RobotMovementPersistenceGameTests.eeprom("""
                    component.proxy(component.list('eeprom')()).setData('newboot')
                    while true do computer.pullSignal() end
                    """);
                player.setShiftKeyDown(true);
                player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
                helper.assertTrue(host.getBlockState().useItemOn(replacement, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(host))
                    .consumesAction(), "Sneak EEPROM interaction did not swap chip");
                helper.assertTrue(host.machine().isRunning() && host.machine().architecture() == architecture && architecture.isInitialized(),
                    "EEPROM hot-swap stopped/replaced native VM");
                int returned = 0;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    final var stack = player.getInventory().getItem(slot);
                    if (stack.is(ModItems.EEPROM.get())) {
                        helper.assertTrue(new String(ItemDriverData.dataTag(stack).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG),
                            StandardCharsets.UTF_8).equals("ready"), "Returned EEPROM lost latest data");
                        returned += stack.getCount();
                    }
                }
                helper.assertTrue(returned == 1 && host.getItem(2).getCount() == 1, "EEPROM swap lost or duplicated ownership");
                host.machine().signal("after_swap");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(host).equals("continued:731"),
                "Old Lua continuation did not survive chip swap: " + host.machine().lastError()))
            .thenExecute(() -> {
                host.machine().stop();
                helper.assertTrue(host.machine().start(), "Could not restart after EEPROM swap");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(host).equals("newboot"), "Restart did not use newly installed EEPROM code"))
            .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerInspectionRequiresNearbyPlayer(final GameTestHelper helper) {
        final var player = player(helper);
        final var host = fixture(helper, player);
        player.moveTo(Vec3.atCenterOf(host.getBlockPos()));
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(host.getBlockState().useWithoutItem(helper.getLevel(), player, hit(host)).consumesAction()
            && player.containerMenu instanceof MicrocontrollerMenu, "Sneak empty-hand did not open inspection menu");
        helper.assertTrue(player.containerMenu.stillValid(player), "Nearby inspection menu is invalid");
        player.moveTo(Vec3.atCenterOf(host.getBlockPos().east(16)));
        helper.assertTrue(!player.containerMenu.stillValid(player), "Distant player retained microcontroller menu access");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerOffhandEepromSwapReturnsOverflowOnce(final GameTestHelper helper) {
        final var player = player(helper);
        final var host = fixture(helper, player);
        player.moveTo(Vec3.atCenterOf(host.getBlockPos()));
        final var original = host.getItem(2).copy();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(net.minecraft.world.item.Items.STICK, 64));
        }
        final var replacement = RobotMovementPersistenceGameTests.eeprom("-- replacement offhand EEPROM");
        player.setItemInHand(InteractionHand.OFF_HAND, replacement);
        player.setShiftKeyDown(true);
        helper.assertTrue(host.getBlockState().useItemOn(replacement, helper.getLevel(), player, InteractionHand.OFF_HAND, hit(host))
            .consumesAction(), "Offhand EEPROM interaction was not consumed");
        helper.assertTrue(player.getOffhandItem().isEmpty() && host.getItem(2).getCount() == 1,
            "Offhand swap did not transfer one chip");
        helper.assertTrue(new String(ItemDriverData.dataTag(host.getItem(2)).getByteArray(ItemRegistry.EEPROM_CODE_TAG),
            StandardCharsets.UTF_8).equals("-- replacement offhand EEPROM"), "Offhand chip was not installed");
        final var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(host.getBlockPos()).inflate(2), entity -> entity.getItem().is(ModItems.EEPROM.get()));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
            "Full inventory lost or duplicated returned EEPROM");
        helper.assertTrue(ItemStack.matches(original, drops.getFirst().getItem()), "Returned EEPROM lost its component data");
        helper.assertTrue(player.getInventory().items.stream().allMatch(stack -> stack.is(net.minecraft.world.item.Items.STICK)
            && stack.getCount() == 64), "EEPROM overflow modified full inventory");
        helper.succeed();
    }

    private static MicrocontrollerBlockEntity fixture(final GameTestHelper helper, final ServerPlayer player) {
        return MicrocontrollerItemRoundTripGameTests.place(helper, player,
            MicrocontrollerItemRoundTripGameTests.assemble(helper, new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get())), new BlockPos(1, 0, 1));
    }

    private static String marker(final MicrocontrollerBlockEntity host) {
        return new String(ItemDriverData.dataTag(host.getItem(2)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }

    private static BlockHitResult hit(final MicrocontrollerBlockEntity host) {
        return new BlockHitResult(Vec3.atCenterOf(host.getBlockPos()), Direction.NORTH, host.getBlockPos(), false);
    }

    private static ServerPlayer player(final GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateAdHocChannels(player.connection.getConnection())
            .add(ResourceLocation.fromNamespaceAndPath("neoforge", "advanced_open_screen"));
        helper.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener() {
            @Override public void testStructureLoaded(final net.minecraft.gametest.framework.GameTestInfo test) { }
            @Override public void testAddedForRerun(final net.minecraft.gametest.framework.GameTestInfo test,
                                                   final net.minecraft.gametest.framework.GameTestInfo rerun,
                                                   final net.minecraft.gametest.framework.GameTestRunner runner) { }
            @Override public void testPassed(final net.minecraft.gametest.framework.GameTestInfo test,
                                             final net.minecraft.gametest.framework.GameTestRunner runner) { cleanup(); }
            @Override public void testFailed(final net.minecraft.gametest.framework.GameTestInfo test,
                                             final net.minecraft.gametest.framework.GameTestRunner runner) { cleanup(); }
            private void cleanup() { player.closeContainer(); helper.getLevel().getServer().getPlayerList().remove(player); }
        });
        return player;
    }
}
