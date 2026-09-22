package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.menu.TabletMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletEditorGameTests {
    @GameTest(template = "empty")
    public static void tabletEditorChangesOnlyExpansionSlot(GameTestHelper helper) { edit(helper, false); }

    @GameTest(template = "empty")
    public static void offhandTabletEditorLocksOffhandSwap(GameTestHelper helper) { edit(helper, true); }

    private static void edit(GameTestHelper helper, boolean offhand) {
        final var player = helper.makeMockServerPlayerInLevel();
        enableMenuChannel(player);
        player.setNoGravity(true);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            new ItemStack(ModItems.CARD_CONTAINER_TIER1.get()), new ItemStack(ModItems.CPU_TIER1.get()),
            new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
        final var hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, stack);
        final var runtime = TabletRuntimeRegistry.get(stack, player);
        helper.assertTrue(runtime.start(), "Tablet did not start");
        player.setShiftKeyDown(true);
        item.use(player.level(), player, hand);
        player.releaseUsingItem();
        helper.assertTrue(player.containerMenu instanceof TabletMenu, "Tier2 sneak use did not open component editor");
        final var menu = (TabletMenu) player.containerMenu;
        helper.assertTrue(!runtime.machine().isRunning(), "Editor left tablet running");
        helper.assertTrue(menu.slots.size() == 37, "Editor exposes more than one component slot");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.CPU_TIER1.get())), "Expansion accepted CPU");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.NETWORK_CARD.get())), "Expansion accepted blacklisted wired card");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get())), "Expansion accepted higher-tier card");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.SCREEN_TIER1.get())), "Expansion accepted screen");
        final int held = offhand ? 40 : player.getInventory().selected;
        menu.clicked(0, held, ClickType.SWAP, player);
        helper.assertTrue(player.getItemInHand(hand) == stack, "Hotbar/offhand swap removed open tablet");
        if (!offhand) {
            final int tabletSlot = menu.findSlot(player.getInventory(), held).orElseThrow();
            helper.assertTrue(!menu.getSlot(tabletSlot).mayPickup(player), "Held tablet slot is not locked");
            menu.clicked(tabletSlot, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty() && player.getItemInHand(hand) == stack, "Editor allowed picking up its tablet");
            helper.assertTrue(menu.quickMoveStack(player, tabletSlot).isEmpty(), "Editor shift-moved its tablet");
        }
        player.getInventory().setItem(9, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get(), 2));
        final int source = menu.findSlot(player.getInventory(), 9).orElseThrow();
        helper.assertTrue(menu.stillValid(player), "Editor became invalid: closed=" + runtime.isClosed() + ", running=" + runtime.machine().isRunning() + ", held=" + (player.getItemInHand(hand) == runtime.stack()));
        helper.assertTrue(TabletMenu.acceptsExpansion(stack, player.getInventory().getItem(9)), "Tablet rejects tier1 card driver");
        helper.assertTrue(menu.getSlot(0).mayPlace(player.getInventory().getItem(9)), "Editor slot rejects valid card");
        menu.clicked(source, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().is(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()) && menu.getSlot(0).getItem().getCount() == 1, "Expansion did not receive exactly one card: slot=" + menu.getSlot(0).getItem() + ", source=" + player.getInventory().getItem(9));
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 1, "Expansion duplicated or consumed spare card");
        helper.assertTrue(item.getComponent(stack, 31).is(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()), "Expansion was not saved to tablet");
        helper.assertTrue(runtime.machine().components().containsValue("modem"), "Inserted card not connected to tablet network");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && item.getComponent(stack, 31).isEmpty(), "Expansion removal was not saved");
        helper.assertTrue(!runtime.machine().components().containsValue("modem"), "Removed card stayed connected");
        helper.assertTrue(item.getComponent(stack, 1).is(ModItems.CPU_TIER1.get()), "Editor changed fixed CPU");
        player.setItemInHand(hand, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player), "Editor remained valid after tablet removal");
        player.closeContainer();
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }

    static void enableMenuChannel(net.minecraft.server.level.ServerPlayer player) {
        // Embedded mock connections do not run the NeoForge client handshake.
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateAdHocChannels(player.connection.getConnection())
            .add(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("neoforge", "advanced_open_screen"));
    }

    @GameTest(template = "empty")
    public static void tier1TabletSneakUseOnlyStops(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER1.get()), ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        item.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.releaseUsingItem();
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Tier1 tablet opened component editor");
        helper.assertTrue(!TabletMenu.acceptsExpansion(stack, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get())), "Tablet without container accepts expansion");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }
}
