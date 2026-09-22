package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DronePickupGameTests {
    @GameTest(template = "empty")
    public static void fallingBelowWorldPacksDroneOnlyOnce(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        drone.moveTo(pos.getX() + 0.5, helper.getLevel().getMinBuildHeight() - 65, pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        drone.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        final var bounds = drone.getBoundingBox().inflate(1);
        drone.tick();
        helper.assertTrue(drone.isRemoved(), "Void drone was not removed");
        drone.tick();
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(ModItems.DRONE.get())).count() == 1,
            "Void removal lost or duplicated packed drone");
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(Items.DIAMOND))
            .mapToInt(entity -> entity.getItem().getCount()).sum() == 7, "Void removal lost or duplicated cargo");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchPickupConservesHardwareCargoAndEnergy(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get()),
            new ItemStack(ModItems.GENERATOR_UPGRADE.get()),
            new ItemStack(ModItems.CPU_TIER1.get()), new ItemStack(ModItems.MEMORY_TIER1.get()),
            RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end")), null);
        helper.getLevel().addFreshEntity(drone);
        drone.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        drone.mainInventory().setItem(1, new ItemStack(Items.COAL, 3));
        drone.setSelectedSlot(1);
        helper.assertTrue(Boolean.TRUE.equals(generator(drone).invoke("insert", drone.machine(), 3)[0]), "Generator did not accept test fuel");
        final var connector = (Connector) drone.machine().node();
        connector.changeBuffer(231 - connector.localBuffer());
        helper.assertTrue(connector.localBuffer() == 231, "Energy fixture did not initialize");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        drone.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(drone.machine().isRunning(), "Sneak interaction did not start drone");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        drone.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(drone.isRemoved() && !drone.machine().isRunning(), "Wrench pickup did not remove and stop drone");
        drone.interact(player, InteractionHand.MAIN_HAND);
        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        final var droneDrops = drops.stream().filter(entity -> entity.getItem().is(ModItems.DRONE.get())).toList();
        helper.assertTrue(droneDrops.size() == 1 && droneDrops.getFirst().getItem().getCount() == 1, "Pickup duplicated or lost drone item");
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(Items.DIAMOND))
            .mapToInt(entity -> entity.getItem().getCount()).sum() == 7, "Pickup duplicated or lost cargo");
        final var packed = droneDrops.getFirst().getItem();
        helper.assertTrue(((DroneItem) packed.getItem()).componentStacks(packed).size() == 5, "Pickup lost installed hardware");
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(Items.COAL))
            .mapToInt(entity -> entity.getItem().getCount()).sum() == 3, "Generator fuel was not dropped exactly once");
        final var restored = new DroneEntity(helper.getLevel());
        restored.loadFromItemStack(packed, null);
        helper.assertTrue(restored.tier() == 1 && restored.mainInventory().getContainerSize() == 4, "Pickup changed drone tier or capacity");
        helper.assertTrue(restored.mainInventory().isEmpty(), "Packed drone duplicated dropped cargo");
        helper.assertTrue(((Number) generator(restored).invoke("count", restored.machine())[0]).intValue() == 0, "Packed generator duplicated dropped fuel");
        helper.assertTrue(((Connector) restored.machine().node()).localBuffer() == 231,
            "Pickup did not conserve machine energy: packed=" + ((DroneItem) packed.getItem()).storedEnergy(packed, -1)
                + ", restored=" + ((Connector) restored.machine().node()).localBuffer());
        helper.assertTrue(!restored.machine().isRunning(), "Replaced drone resumed running unexpectedly");
        restored.discard();
        helper.succeed();
    }

    private static li.cil.oc.api.network.Component generator(DroneEntity drone) {
        for (var node : drone.machine().node().reachableNodes()) {
            if (node instanceof li.cil.oc.api.network.Component component && component.name().equals("generator")) return component;
        }
        throw new AssertionError("Generator missing");
    }
}
