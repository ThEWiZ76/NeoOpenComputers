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
import li.cil.oc.api.internal.Robot;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class AgentSuckGameTests {
    @GameTest(template = "empty")
    public static void robotDoesNotPickUpEntityRemovedByPickupEvent(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        final var target = helper.absolutePos(pos).relative(robot.toGlobal(Direction.SOUTH));
        final var item = new ItemEntity(helper.getLevel(), target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
            new ItemStack(Items.DIAMOND, 7));
        item.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(item);
        java.util.function.Consumer<ItemEntityPickupEvent.Pre> remove = event -> {
            if (event.getItemEntity() == item) item.discard();
        };
        NeoForge.EVENT_BUS.addListener(remove);
        try {
            final var result = ((Component) robot.node()).invoke("suck", robot.machine(), 3);
            helper.assertTrue(Boolean.FALSE.equals(result[0]) && robot.mainInventory().isEmpty(),
                "Robot duplicated an item already removed by a pickup handler");
        } finally {
            NeoForge.EVENT_BUS.unregister(remove);
            item.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSucksInventoryAndWorldItems(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneSucksInventoryAndWorldItems(GameTestHelper helper) throws Exception {
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
        final var inventory = agent.mainInventory();
        final var component = (Component) ((Environment) agent).node();
        agent.setSelectedSlot(1);
        world.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        final var chest = (ChestBlockEntity) world.getBlockEntity(target);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 11));
        expectCount(helper, component.invoke("suck", agent.machine(), side, 3), 3);
        helper.assertTrue(chest.getItem(0).getCount() == 8 && inventory.getItem(1).getCount() == 3
            && inventory.getItem(0).isEmpty(), "Extraction did not start at selected slot");
        java.util.function.Consumer<PlayerInteractEvent.RightClickBlock> deny = event -> {
            if (event.getEntity() == agent.player() && event.getPos().equals(target)) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(deny);
        try { helper.assertTrue(Boolean.FALSE.equals(component.invoke("suck", agent.machine(), side, 3)[0]), "Protected extraction succeeded"); }
        finally { NeoForge.EVENT_BUS.unregister(deny); }
        helper.assertTrue(chest.getItem(0).getCount() == 8 && inventory.getItem(1).getCount() == 3, "Protected extraction changed items");
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 63));
        expectCount(helper, component.invoke("suck", agent.machine(), side, 5), 1);
        helper.assertTrue(chest.getItem(0).getCount() == 7 && inventory.getItem(1).getCount() == 64, "Partial extraction lost remainder");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("suck", agent.machine(), side)[0]), "Full cargo extracted items");
        helper.assertTrue(chest.getItem(0).getCount() == 7, "Full cargo consumed source");
        inventory.clearContent();
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("suck", agent.machine(), side, 0)[0]), "Zero inventory extraction succeeded");
        chest.clearContent();
        world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        final var item = new ItemEntity(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, new ItemStack(Items.IRON_INGOT, 5));
        item.setPickUpDelay(15);
        world.addFreshEntity(item);
        try {
            helper.assertTrue(Boolean.FALSE.equals(component.invoke("suck", agent.machine(), side)[0]), "Pickup delay was ignored");
            item.setNoPickUpDelay();
            expectCount(helper, component.invoke("suck", agent.machine(), side, 1), 5);
            helper.assertTrue(item.isRemoved() && inventory.getItem(1).is(Items.IRON_INGOT)
                && inventory.getItem(1).getCount() == 5, "World pickup lost items or selection order");
        } finally { item.discard(); }
        inventory.clearContent();
        final var cart = EntityType.CHEST_MINECART.create(world);
        cart.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0, 0);
        cart.setItem(0, new ItemStack(Items.DIAMOND, 7));
        helper.assertTrue(world.addFreshEntity(cart), "Minecart fixture did not spawn");
        try {
            java.util.function.Consumer<PlayerInteractEvent.EntityInteract> denyEntity = event -> {
                if (event.getEntity() == agent.player() && event.getTarget() == cart) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(denyEntity);
            try { helper.assertTrue(Boolean.FALSE.equals(component.invoke("suck", agent.machine(), side, 2)[0]), "Protected minecart extraction succeeded"); }
            finally { NeoForge.EVENT_BUS.unregister(denyEntity); }
            helper.assertTrue(inventory.isEmpty() && cart.getItem(0).getCount() == 7, "Protected minecart changed cargo");
            expectCount(helper, component.invoke("suck", agent.machine(), side, 2), 2);
            helper.assertTrue(cart.getItem(0).getCount() == 5 && inventory.getItem(1).getCount() == 2,
                "Minecart extraction did not conserve cargo");
        } finally {
            cart.clearContent();
            cart.discard();
        }
        inventory.clearContent();
        final var localItem = new ItemEntity(world, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, new ItemStack(Items.DIAMOND, 3));
        final var neighborItem = new ItemEntity(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, new ItemStack(Items.IRON_INGOT, 4));
        localItem.setNoPickUpDelay();
        neighborItem.setNoPickUpDelay();
        world.addFreshEntity(localItem);
        world.addFreshEntity(neighborItem);
        try {
            expectCount(helper, component.invoke("suck", agent.machine(), side), agent instanceof Robot ? 4 : 3);
            helper.assertTrue(agent instanceof Robot ? localItem.isAlive() && neighborItem.isRemoved()
                : localItem.isRemoved() && neighborItem.isAlive(), "Wrong world pickup priority");
        } finally {
            localItem.discard();
            neighborItem.discard();
        }
    }

    private static void expectCount(GameTestHelper helper, Object[] result, int count) {
        helper.assertTrue(result[0] instanceof Number value && value.intValue() == count,
            "Expected pickup count " + count + ", got " + java.util.Arrays.toString(result));
    }
}
