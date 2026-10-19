package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class InventoryControllerTransferGameTests {
    @GameTest(template = "empty")
    public static void robotControllerTargetsSlotsWithoutLosingPartialStacks(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneControllerTargetsSlotsWithoutLosingPartialStacks(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        try { verify(helper, drone, 5, Direction.EAST); }
        finally { drone.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotControllerHonorsRotationAndSidedCapabilities(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get().defaultBlockState()
            .setValue(li.cil.oc.common.block.RobotBlock.FACING, Direction.EAST));
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var target = robot.getBlockPos().east();
        helper.getLevel().setBlockAndUpdate(target, Blocks.FURNACE.defaultBlockState());
        final var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) helper.getLevel().getBlockEntity(target);
        final var component = controller(robot);
        robot.mainInventory().setItem(0, new ItemStack(Items.IRON_ORE, 8));
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("dropIntoSlot", null, 3, 1, 4, 1)[0])
            && furnace.getItem(0).getCount() == 4 && robot.mainInventory().getItem(0).getCount() == 4,
            "Facing must be robot-relative, fromSide must be the requested world side");
        robot.mainInventory().setItem(0, new ItemStack(Items.COAL, 6));
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("dropIntoSlot", null, 3, 1, 3, 1)[0])
            && furnace.getItem(1).isEmpty(), "Top capability bypassed furnace slot restrictions");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("dropIntoSlot", null, 3, 1, 3, 2)[0])
            && furnace.getItem(1).getCount() == 3, "Explicit side did not expose the fuel slot");
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        helper.assertTrue(((Number) component.invoke("suckFromSlot", null, 3, 1, 2, 0)[0]).intValue() == 2
            && furnace.getItem(2).getCount() == 3 && robot.mainInventory().getItem(1).getCount() == 2,
            "Sided output extraction failed");
        for (int side : new int[]{2, 4, 5, 6, -1}) {
            try { component.invoke("dropIntoSlot", null, side, 1); throw new AssertionError("Robot accepted invalid action side"); }
            catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().contains("invalid side"), "Wrong side error"); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerHonorsCanceledAndDeniedBlockAccessWithoutWorldDrops(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get().defaultBlockState()
            .setValue(li.cil.oc.common.block.RobotBlock.FACING, Direction.SOUTH));
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var target = robot.getBlockPos().south();
        helper.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        final var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(target);
        robot.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 9));
        final var component = controller(robot);
        final boolean[] deny = {false};
        final Object protection = new Object() {
            @SubscribeEvent public void access(PlayerInteractEvent.RightClickBlock event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(target)) {
                    if (deny[0]) event.setUseBlock(TriState.FALSE); else event.setCanceled(true);
                }
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            for (boolean denyBlock : new boolean[]{false, true}) {
                deny[0] = denyBlock;
                for (String method : new String[]{"dropIntoSlot", "suckFromSlot"}) {
                    helper.assertTrue(java.util.Arrays.equals(component.invoke(method, null, 3, 1), new Object[]{null, "no inventory"}),
                        "Protected inventory returned an unexpected result");
                }
                helper.assertTrue(robot.mainInventory().getItem(0).getCount() == 7 && chest.getItem(0).getCount() == 9,
                    "Protected inventory was mutated");
            }
        } finally { NeoForge.EVENT_BUS.unregister(protection); }
        helper.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        final var loose = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
            target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, new ItemStack(Items.EMERALD, 3));
        loose.setNoPickUpDelay(); helper.getLevel().addFreshEntity(loose);
        final int before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(target)).size();
        helper.assertTrue(component.invoke("dropIntoSlot", null, 3, 1)[0] == null
            && component.invoke("suckFromSlot", null, 3, 1)[0] == null
            && robot.mainInventory().getItem(0).getCount() == 7 && loose.getItem().getCount() == 3,
            "Targeted operation fell back to loose items");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(target)).size() == before, "Targeted drop spawned an item");
        loose.discard(); helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeRobotProgramUsesInstalledControllerTransfers(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get().defaultBlockState()
            .setValue(li.cil.oc.common.block.RobotBlock.FACING, Direction.SOUTH));
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        robot.setTier(1);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu,
            li.cil.oc.common.machine.NativeLuaArchitecture.class);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(cpu,
            new ItemStack(ModItems.MEMORY_TIER1.get()), new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()),
            RobotMovementPersistenceGameTests.eeprom("""
                local inv = component.proxy(component.list('inventory_controller')())
                local robot = component.proxy(component.list('robot')())
                assert(inv.dropIntoSlot(3, 2, 4))
                assert(inv.suckFromSlot(3, 2, 2) == 2)
                robot.setLightColor(0x12AB34)
                while true do computer.pullSignal() end
                """)));
        robot.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 9));
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(1, 1, 2));
        robot.onLoad();
        ((li.cil.oc.api.network.Connector) robot.machine().node()).changeBuffer(10000);
        helper.assertTrue(robot.machine().start(), "Native robot did not start");
        helper.succeedWhen(() -> helper.assertTrue(robot.lightColor() == 0x12AB34
            && chest.getItem(1).getCount() == 2 && robot.mainInventory().getItem(0).getCount() == 7,
            "Native installed-controller transfers failed: " + robot.machine().lastError()));
    }

    static Component controller(Agent agent) {
        final var stack = new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get());
        return (Component) Driver.driverFor(stack).createEnvironment(stack, agent).node();
    }

    @GameTest(template = "empty")
    public static void droneControllerUsesProtectedEntityInventory(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        final var cart = helper.spawn(net.minecraft.world.entity.EntityType.CHEST_MINECART, new BlockPos(2, 1, 1));
        cart.setNoGravity(true);
        drone.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 8));
        final var component = controller(drone);
        final Object protection = new Object() {
            @SubscribeEvent public void access(PlayerInteractEvent.EntityInteract event) {
                if (event.getTarget() == cart) event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            helper.assertTrue(java.util.Arrays.equals(component.invoke("dropIntoSlot", null, 5, 2, 4), new Object[]{null, "no inventory"})
                && cart.isEmpty() && drone.mainInventory().getItem(0).getCount() == 8, "Entity protection was bypassed");
        } finally { NeoForge.EVENT_BUS.unregister(protection); }
        try {
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("dropIntoSlot", null, 5, 2, 4)[0])
                && cart.getItem(1).getCount() == 4, "Entity inventory was not found");
            helper.assertTrue(((Number) component.invoke("suckFromSlot", null, 5, 2, 2)[0]).intValue() == 2
                && cart.getItem(1).getCount() == 2 && drone.mainInventory().getItem(0).getCount() == 6,
                "Entity extraction lost items");
        } finally { cart.discard(); drone.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeDroneProgramUsesInstalledControllerTransfers(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu,
            li.cil.oc.common.machine.NativeLuaArchitecture.class);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), cpu, new ItemStack(ModItems.MEMORY_TIER1.get()),
            new ItemStack(ModItems.INVENTORY_UPGRADE.get()), new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()),
            RobotMovementPersistenceGameTests.eeprom("""
                local inv = component.proxy(component.list('inventory_controller')())
                assert(inv.dropIntoSlot(5, 2, 4))
                assert(inv.suckFromSlot(5, 2, 2) == 2)
                component.proxy(component.list('drone')()).setLightColor(0x12AB34)
                while true do computer.pullSignal() end
                """)), null);
        helper.getLevel().addFreshEntity(drone);
        drone.setNoGravity(true);
        drone.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 9));
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(2, 2, 1));
        ((li.cil.oc.api.network.Connector) drone.machine().node()).changeBuffer(10000);
        helper.assertTrue(drone.machine().start(), "Native drone did not start");
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(((Number) drone.getLightColor(null, null)[0]).intValue() == 0x12AB34 && chest.getItem(1).getCount() == 2
                && drone.mainInventory().getItem(0).getCount() == 7,
                "Native drone controller failed: " + drone.machine().lastError());
        }).thenExecute(drone::discard).thenSucceed();
    }

    private static void verify(GameTestHelper helper, Agent agent, int side, Direction direction) throws Exception {
        final var component = controller(agent);
        final var pauses = new java.util.ArrayList<Double>();
        final var context = (li.cil.oc.api.machine.Context) java.lang.reflect.Proxy.newProxyInstance(
            li.cil.oc.api.machine.Context.class.getClassLoader(), new Class<?>[]{li.cil.oc.api.machine.Context.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("pause")) { pauses.add((Double) arguments[0]); return true; }
                throw new AssertionError("Unexpected context call " + method.getName());
            });
        final var pos = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(direction);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        final var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var inventory = agent.mainInventory();
        agent.setSelectedSlot(1);
        final var named = new ItemStack(Items.DIAMOND, 10);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("transfer-probe"));
        inventory.setItem(1, named);
        chest.setItem(2, named.copyWithCount(63));
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("dropIntoSlot", context, side, 3, 5)[0]), "Partial targeted drop failed");
        helper.assertTrue(inventory.getItem(1).getCount() == 9 && chest.getItem(2).getCount() == 64
            && chest.getItem(0).isEmpty(), "Drop lost items or used another slot");
        final var full = component.invoke("dropIntoSlot", context, side, 3);
        helper.assertTrue(Boolean.FALSE.equals(full[0]) && full[1].equals("inventory full/invalid slot"), "Full slot error changed");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("dropIntoSlot", null, side, 1, 0)[0])
            && inventory.getItem(1).getCount() == 9, "Zero count mutated inventory");
        helper.assertTrue(((Number) component.invoke("suckFromSlot", context, side, 3, 5)[0]).intValue() == 5, "Targeted suck count incorrect");
        helper.assertTrue(pauses.equals(java.util.List.of(li.cil.oc.common.ModSettings.robotDropDelay(),
            li.cil.oc.common.ModSettings.robotSuckDelay())), "Successful transfers must pause once; rejected transfers must not pause");
        helper.assertTrue(inventory.getItem(1).getCount() == 14 && chest.getItem(2).getCount() == 59
            && ItemStack.isSameItemSameComponents(inventory.getItem(1), named), "Suck lost count, selection order or item data");
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, named.copyWithCount(64));
        inventory.setItem(1, named.copyWithCount(63));
        helper.assertTrue(((Number) component.invoke("suckFromSlot", null, side, 3, 64)[0]).intValue() == 1
            && chest.getItem(2).getCount() == 58, "Simulation extracted more than internal capacity");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("suckFromSlot", null, side, 3)[0])
            && chest.getItem(2).getCount() == 58, "Full internal inventory consumed source items");
        for (int slot : new int[]{0, 28}) {
            try {
                component.invoke("suckFromSlot", null, side, slot);
                throw new AssertionError("Invalid slot accepted");
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(expected.getMessage().contains("invalid slot"), "Wrong invalid slot error");
            }
        }
        inventory.setItem(1, named.copyWithCount(2));
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("dropIntoSlot", null, side, 1, null)[0])
            && inventory.getItem(1).isEmpty() && chest.getItem(0).getCount() == 2,
            "Explicit nil count must use the default and empty the source slot");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("dropIntoSlot", null, side, 0)[0]),
            "Empty source must return false before inspecting the target slot");
    }

    private InventoryControllerTransferGameTests() { }
}
