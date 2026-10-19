package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ItemInventoryControlGameTests {
    @GameTest(template = "empty")
    public static void robotItemInventoryTransfersPreserveCountsAndData(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        verify(helper, robot);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneItemInventoryTransfersPreserveCountsAndData(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        try { verify(helper, drone); } finally { drone.discard(); }
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent) throws Exception {
        final var component = InventoryControllerTransferGameTests.controller(agent);
        final var inventory = agent.mainInventory();
        inventory.setItem(0, new ItemStack(Items.BLUE_SHULKER_BOX));
        agent.setSelectedSlot(1);
        final var named = new ItemStack(Items.DIAMOND, 9);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("item-inventory-probe"));
        inventory.setItem(1, named);
        helper.assertTrue(((Number) component.invoke("getItemInventorySize", null, 1)[0]).intValue() == 27,
            "Modern item capability was not exposed");
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 3, 4)[0]).intValue() == 4,
            "Item inventory drop count incorrect");
        helper.assertTrue(inventory.getItem(1).getCount() == 5 && contents(agent, 0).getStackInSlot(2).getCount() == 4,
            "Drop did not write back into the container item");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 3, 2)[0]).intValue() == 2
            && inventory.getItem(1).getCount() == 7 && contents(agent, 0).getStackInSlot(2).getCount() == 2,
            "Suck did not update source and destination counts");
        helper.assertTrue(ItemStack.isSameItemSameComponents(inventory.getItem(1), named), "Transfer lost item data");
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, named.copyWithCount(64));
        inventory.setItem(1, named.copyWithCount(63));
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 3)[0]).intValue() == 1
            && contents(agent, 0).getStackInSlot(2).getCount() == 1, "Partial capacity over-extracted the item inventory");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 3)[0]).intValue() == 0
            && contents(agent, 0).getStackInSlot(2).getCount() == 1, "Full cargo consumed stored items");
        agent.setSelectedSlot(0);
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 1)[0]).intValue() == 0
            && inventory.getItem(0).is(Items.BLUE_SHULKER_BOX), "Container inserted into itself");
        final ItemStack encoded = ItemStack.parseOptional(helper.getLevel().registryAccess(),
            (CompoundTag) inventory.getItem(0).save(helper.getLevel().registryAccess()));
        helper.assertTrue(Driver.itemHandlerFor(encoded, agent.player()).getStackInSlot(2).getCount() == 1,
            "Item serialization lost stored inventory");
        final var noInventory = component.invoke("getItemInventorySize", null, 2);
        helper.assertTrue(java.util.Arrays.equals(noInventory, new Object[]{0, "no item inventory"}), "Missing inventory result changed");
        for (int slot : new int[]{0, 28}) {
            try { component.invoke("suckFromItemInventory", null, 1, slot); throw new AssertionError("Invalid item slot accepted"); }
            catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().contains("invalid slot"), "Wrong slot error"); }
        }
    }

    private static net.neoforged.neoforge.items.IItemHandler contents(Agent agent, int slot) {
        return Driver.itemHandlerFor(agent.mainInventory().getItem(slot), agent.player());
    }

    @GameTest(template = "empty")
    public static void databaseDescriptionsCannotBeExtractedAsRealItems(GameTestHelper helper) throws Exception {
        final var stack = new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get());
        final var database = (li.cil.oc.common.component.DatabaseEnvironment) Driver.driverFor(stack).createEnvironment(stack, null);
        database.setStackInSlot(0, new ItemStack(Items.DIAMOND, 64));
        database.save(new CompoundTag());
        helper.assertTrue(Driver.itemHandlerFor(stack, null).extractItem(0, 64, true).isEmpty(),
            "A database description materialized real items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void databasePhysicalItemsRoundTripButCopiesStayDescriptions(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var inventory = robot.mainInventory();
        inventory.setItem(0, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 8)); robot.setSelectedSlot(1);
        final var component = InventoryControllerTransferGameTests.controller(robot);
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 2, 5)[0]).intValue() == 1
            && inventory.getItem(1).getCount() == 7, "Database insertion ignored its one-item limit");
        final var database = (li.cil.oc.common.component.DatabaseEnvironment) Driver.driverFor(inventory.getItem(0))
            .createEnvironment(inventory.getItem(0), null);
        ((li.cil.oc.api.network.Component) database.node()).invoke("copy", null, 2, 3);
        database.save(new CompoundTag());
        helper.assertTrue(database.getStackInSlot(2).is(Items.DIAMOND)
            && ((Number) component.invoke("suckFromItemInventory", null, 1, 3)[0]).intValue() == 0,
            "Copying a database entry duplicated the physical item");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 2)[0]).intValue() == 1
            && inventory.getItem(1).getCount() == 8, "Physically inserted database item was lost");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 2)[0]).intValue() == 0,
            "Physical database item could be extracted twice");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stackedContainerItemsAreSeparatedBeforeWritingContents(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var inventory = robot.mainInventory();
        inventory.setItem(0, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get(), 2));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 4)); robot.setSelectedSlot(1);
        final var component = InventoryControllerTransferGameTests.controller(robot);
        helper.assertTrue(Driver.itemHandlerFor(inventory.getItem(0), robot.player()).insertItem(0,
            new ItemStack(Items.DIAMOND), false).getCount() == 1,
            "Direct item-provider write duplicated contents across stacked containers");
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 1)[0]).intValue() == 1,
            "Stacked container did not accept a transfer into one item");
        helper.assertTrue(inventory.getItem(0).getCount() == 1 && inventory.getItem(2).getCount() == 1
            && inventory.getItem(2).is(ModItems.DATABASE_UPGRADE_TIER1.get())
            && Driver.itemHandlerFor(inventory.getItem(2), robot.player()).getStackInSlot(0).isEmpty(),
            "Writing one container copied its new contents to all stacked containers");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 1)[0]).intValue() == 1
            && inventory.getItem(1).getCount() == 4, "Stacked-container round trip lost or duplicated items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverItemTransfersRespectHardwareSlotsAndPersistData(GameTestHelper helper) throws Exception {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var inventory = robot.mainInventory();
        inventory.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu,
            li.cil.oc.common.machine.NativeLua54Architecture.class);
        inventory.setItem(1, cpu); robot.setSelectedSlot(1);
        final var component = InventoryControllerTransferGameTests.controller(robot);
        helper.assertTrue(((Number) component.invoke("getItemInventorySize", null, 1)[0]).intValue() == 9, "Server size changed");
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 1)[0]).intValue() == 0,
            "CPU entered a server card slot");
        helper.assertTrue(((Number) component.invoke("dropIntoItemInventory", null, 1, 3)[0]).intValue() == 1
            && inventory.getItem(1).isEmpty(), "CPU did not enter the server CPU slot");
        final var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
            (CompoundTag) inventory.getItem(0).save(helper.getLevel().registryAccess()));
        helper.assertTrue(Driver.itemHandlerFor(restored, robot.player()).getStackInSlot(2).is(ModItems.CPU_TIER1.get()),
            "Server hardware was not persisted into its item");
        helper.assertTrue(((Number) component.invoke("suckFromItemInventory", null, 1, 3)[0]).intValue() == 1
            && inventory.getItem(1).is(ModItems.CPU_TIER1.get())
            && ((li.cil.oc.api.driver.item.Processor) Driver.driverFor(inventory.getItem(1))).architecture(inventory.getItem(1))
                == li.cil.oc.common.machine.NativeLua54Architecture.class, "Extracted CPU lost architecture metadata");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeRobotProgramUsesItemInventoryCallbacks(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1)); robot.setTier(1);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(nativeCpu(),
            new ItemStack(ModItems.MEMORY_TIER1.get()), new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()),
            RobotMovementPersistenceGameTests.eeprom(program("robot"))));
        robot.mainInventory().setItem(0, new ItemStack(Items.SHULKER_BOX));
        robot.mainInventory().setItem(1, new ItemStack(Items.DIAMOND, 9)); robot.setSelectedSlot(1);
        robot.onLoad(); ((li.cil.oc.api.network.Connector) robot.machine().node()).changeBuffer(10000);
        helper.assertTrue(robot.machine().start(), "Native robot did not start");
        helper.succeedWhen(() -> helper.assertTrue(robot.lightColor() == 0x12AB34
            && robot.mainInventory().getItem(1).getCount() == 7 && contents(robot, 0).getStackInSlot(2).getCount() == 2,
            "Native item callbacks failed: " + robot.machine().lastError()));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeDroneProgramUsesItemInventoryCallbacks(GameTestHelper helper) {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), nativeCpu(), new ItemStack(ModItems.MEMORY_TIER1.get()),
            new ItemStack(ModItems.INVENTORY_UPGRADE.get()), new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()),
            RobotMovementPersistenceGameTests.eeprom(program("drone"))), null);
        helper.getLevel().addFreshEntity(drone); drone.setNoGravity(true);
        drone.mainInventory().setItem(0, new ItemStack(Items.SHULKER_BOX));
        drone.mainInventory().setItem(1, new ItemStack(Items.DIAMOND, 9)); drone.setSelectedSlot(1);
        ((li.cil.oc.api.network.Connector) drone.machine().node()).changeBuffer(10000);
        helper.assertTrue(drone.machine().start(), "Native drone did not start");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
            ((Number) drone.getLightColor(null, null)[0]).intValue() == 0x12AB34
                && drone.mainInventory().getItem(1).getCount() == 7 && contents(drone, 0).getStackInSlot(2).getCount() == 2,
            "Native drone item callbacks failed: " + drone.machine().lastError())).thenExecute(drone::discard).thenSucceed();
    }

    private static ItemStack nativeCpu() {
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu,
            li.cil.oc.common.machine.NativeLuaArchitecture.class);
        return cpu;
    }

    private static String program(String host) {
        return """
            local inv = component.proxy(component.list('inventory_controller')())
            assert(inv.getItemInventorySize(1) == 27)
            assert(inv.dropIntoItemInventory(1, 3, 4) == 4)
            assert(inv.suckFromItemInventory(1, 3, 2) == 2)
            component.proxy(component.list('%s')()).setLightColor(0x12AB34)
            while true do computer.pullSignal() end
            """.formatted(host);
    }

    private ItemInventoryControlGameTests() { }
}
