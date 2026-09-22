package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.menu.DroneMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneMenuGameTests {
    @GameTest(template = "empty")
    public static void droneMenuMovesCargoWithoutEditingHardware(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var menu = new DroneMenu(1, player.getInventory(), drone);
        helper.assertTrue(menu.slots.size() == 44, "Drone menu must expose eight cargo slots and player inventory");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Drone menu exposes hardware instead of cargo");
        helper.assertTrue(menu.getSlot(3).isActive() && !menu.getSlot(4).isActive(), "Unavailable cargo slots were not disabled");
        helper.assertTrue(!menu.getSlot(4).mayPlace(new ItemStack(Items.DIAMOND)), "Disabled slot accepted cargo");
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 11));
        helper.assertTrue(!menu.quickMoveStack(player, 8).isEmpty(), "Shift-click into drone failed");
        helper.assertTrue(drone.mainInventory().getItem(0).getCount() == 11, "Shift-click did not reach cargo");
        helper.assertTrue(drone.getItem(0).is(ModItems.INVENTORY_UPGRADE.get()), "Cargo transfer overwrote hardware");
        helper.assertTrue(!menu.quickMoveStack(player, 0).isEmpty(), "Shift-click out of drone failed");
        helper.assertTrue(drone.mainInventory().isEmpty() && player.getInventory().countItem(Items.DIAMOND) == 11, "Cargo transfer lost or duplicated items");
        drone.setItem(1, new ItemStack(ModItems.INVENTORY_UPGRADE.get()));
        helper.assertTrue(menu.getSlot(7).isActive(), "Open menu did not follow increased capacity");
        menu.getSlot(7).set(new ItemStack(Items.IRON_INGOT, 3));
        helper.assertTrue(drone.mainInventory().getItem(7).getCount() == 3, "Open menu retained a stale cargo container");
        final var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            drone.writeClientSideData(menu, buffer);
            final var clientMenu = new DroneMenu(1, player.getInventory(), buffer);
            helper.assertTrue(clientMenu.slots.size() == menu.slots.size() && clientMenu.cargoSize() == 8
                && clientMenu.getSlot(7).isActive(), "Opening data did not initialize client cargo slots");
            clientMenu.setData(DroneMenu.DRONE_CARGO_SIZE_INDEX, 4);
            helper.assertTrue(clientMenu.getSlot(3).isActive() && !clientMenu.getSlot(4).isActive(), "Client data update did not disable excess slots");
            clientMenu.setData(DroneMenu.DRONE_CARGO_SIZE_INDEX, 0);
            helper.assertTrue(!clientMenu.getSlot(0).isActive()
                && !clientMenu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Client enabled cargo without inventory upgrades");
        } finally {
            buffer.release();
        }
        drone.discard();
        helper.succeed();
    }
}
