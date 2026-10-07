package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class AgentDropGameTests {
    @GameTest(template = "empty")
    public static void robotDropsIntoInventoryAndWorld(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneDropsIntoInventoryAndWorld(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        try { verify(helper, drone, 5, Direction.EAST); }
        finally { drone.discard(); }
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent, int side, Direction direction) throws Exception {
        final var origin = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition());
        final var target = origin.relative(direction);
        final var world = helper.getLevel();
        final var component = (Component) ((Environment) agent).node();
        agent.setSelectedSlot(1);
        final var inventory = agent.mainInventory();
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 11));
        world.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        final var chest = (ChestBlockEntity) world.getBlockEntity(target);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("drop", agent.machine(), side, 3)[0]), "Chest insertion failed");
        helper.assertTrue(inventory.getItem(1).getCount() == 8 && chest.getItem(0).getCount() == 3, "Chest insertion lost items");
        for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 63));
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("drop", agent.machine(), side, 3)[0]), "Partial insertion failed");
        helper.assertTrue(inventory.getItem(1).getCount() == 7 && chest.getItem(0).getCount() == 64, "Partial insertion lost remainder");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("drop", agent.machine(), side)[0]), "Full chest accepted drop");
        helper.assertTrue(inventory.getItem(1).getCount() == 7, "Full chest consumed cargo");
        chest.clearContent();
        world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        component.invoke("drop", agent.machine(), side, 0);
        helper.assertTrue(inventory.getItem(1).getCount() == 7, "Zero drop consumed cargo");
        java.util.function.Consumer<ItemTossEvent> cancel = event -> {
            if (event.getPlayer() == agent.player()) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { component.invoke("drop", agent.machine(), side, 2); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(inventory.getItem(1).getCount() == 7, "Canceled drop consumed cargo");
        component.invoke("drop", agent.machine(), side, 2);
        final var items = world.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
        helper.assertTrue(items.size() == 1 && items.getFirst().getItem().is(Items.DIAMOND)
            && items.getFirst().getItem().getCount() == 2 && items.getFirst().hasPickUpDelay(), "World drop duplicated or lost items");
        helper.assertTrue(inventory.getItem(1).getCount() == 5 && agent.selectedSlot() == 1, "World drop changed wrong cargo");
        items.forEach(ItemEntity::discard);

        world.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        final var protectedChest = (ChestBlockEntity) world.getBlockEntity(target);
        java.util.function.Consumer<PlayerInteractEvent.RightClickBlock> denyBlock = event -> {
            if (event.getEntity() == agent.player() && event.getPos().equals(target)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(denyBlock);
        try { component.invoke("drop", agent.machine(), side, 1); }
        finally { NeoForge.EVENT_BUS.unregister(denyBlock); }
        helper.assertTrue(protectedChest.isEmpty() && inventory.getItem(1).getCount() == 4, "Denied chest access inserted or lost cargo");
        final var deniedDrops = world.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
        helper.assertTrue(deniedDrops.size() == 1 && deniedDrops.getFirst().getItem().getCount() == 1,
            "Denied access did not fall back to one world drop");
        deniedDrops.forEach(ItemEntity::discard);
        world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        final var cart = EntityType.CHEST_MINECART.create(world);
        cart.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0, 0);
        helper.assertTrue(world.addFreshEntity(cart), "Chest minecart fixture did not spawn");
        try {
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("drop", agent.machine(), side, 2)[0]), "Minecart insertion failed");
            helper.assertTrue(cart.getItem(0).is(Items.DIAMOND) && cart.getItem(0).getCount() == 2
                && inventory.getItem(1).getCount() == 2, "Minecart insertion did not conserve cargo");
            java.util.function.Consumer<PlayerInteractEvent.EntityInteract> denyEntity = event -> {
                if (event.getEntity() == agent.player() && event.getTarget() == cart) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(denyEntity);
            NeoForge.EVENT_BUS.addListener(cancel);
            try { component.invoke("drop", agent.machine(), side, 1); }
            finally {
                NeoForge.EVENT_BUS.unregister(denyEntity);
                NeoForge.EVENT_BUS.unregister(cancel);
            }
            helper.assertTrue(cart.getItem(0).getCount() == 2 && inventory.getItem(1).getCount() == 2,
                "Canceled entity access/toss consumed or inserted cargo");
        } finally {
            cart.clearContent();
            cart.discard();
        }
    }
}
