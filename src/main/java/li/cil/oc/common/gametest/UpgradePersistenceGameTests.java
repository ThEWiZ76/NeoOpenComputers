package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.NavigationUpgradeEnvironment.NavigationMapData;
import li.cil.oc.common.item.NavigationUpgradeItem;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class UpgradePersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotTractorProxyCollectsItemsAfterReload(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), """
            local upgrade = component.proxy(component.list('tractor_beam')())
            """, """
            assert(upgrade.suck(), 'restored tractor did not collect item')
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotInventoryControllerProxySurvivesReload(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()), """
            local upgrade = component.proxy(component.list('inventory_controller')())
            local stack = upgrade.getStackInInternalSlot(1)
            assert(stack.name == 'minecraft:diamond' and stack.size == 3)
            """, """
            local stack = upgrade.getStackInInternalSlot(1)
            assert(stack.name == 'minecraft:diamond' and stack.size == 3, 'controller lost cargo view')
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotTankControllerProxySurvivesReload(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), """
            local upgrade = component.proxy(component.list('tank_controller')())
            assert(upgrade.getTankLevelInSlot(1) == 1000)
            """, """
            assert(upgrade.getTankLevelInSlot(1) == 1000, 'controller lost water bucket view')
            assert(upgrade.getTankCapacityInSlot(1) == 1000)
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotTankRetainsWaterAcrossReload(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.TANK_UPGRADE.get()), """
            assert(robot.tankLevel(1) == 1000, 'tank fixture not filled')
            """, """
            assert(robot.tankLevel(1) == 1000, 'stored water lost on reload')
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotGeneratorRetainsFuelAcrossReload(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.GENERATOR_UPGRADE.get()), """
            local upgrade = component.proxy(component.list('generator')())
            robot.select(1)
            assert(upgrade.insert(3))
            repeat computer.pullSignal(0.05) until upgrade.count() == 2
            """, """
            assert(upgrade.count() == 2, 'generator queue lost or consumed twice')
            """);
    }

    @GameTest(template = "empty")
    public static void removingGeneratorDoesNotRetainDroppedFuel(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, robot,
            List.of(new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get())));
        robot.onLoad();
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(net.minecraft.world.item.Items.COAL, 3));
        final int slot = 1; // First mutable expansion slot; slot zero holds the tool.
        robot.setItem(slot, new ItemStack(ModItems.GENERATOR_UPGRADE.get()));
        final var generator = generator(robot);
        helper.assertTrue(Boolean.TRUE.equals(generator.invoke("insert", robot.machine(), 3)[0]), "Generator did not accept fuel");
        final var removed = robot.removeItemNoUpdate(slot);
        helper.assertTrue(droppedCoal(helper, robot) == 3, "Generator removal did not drop exactly three coal");
        robot.setItem(slot, removed);
        helper.assertTrue(((Number) generator(robot).invoke("count", robot.machine())[0]).intValue() == 0,
            "Reinserted generator duplicated dropped fuel");
        helper.assertTrue(robot.getItem(RobotBlockEntity.CARGO_SLOT_START).isEmpty(), "Generator insertion left duplicate cargo");
        helper.succeed();
    }

    private static li.cil.oc.api.network.Component generator(RobotBlockEntity robot) {
        for (var node : robot.machine().node().reachableNodes()) {
            if (node instanceof li.cil.oc.api.network.Component component && component.name().equals("generator")) return component;
        }
        throw new AssertionError("Generator component missing");
    }

    private static int droppedCoal(GameTestHelper helper, RobotBlockEntity robot) {
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(robot.getBlockPos()).inflate(0.5)).stream()
            .filter(entity -> entity.getItem().is(net.minecraft.world.item.Items.COAL))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotNavigationProxyRetainsMap(GameTestHelper helper) {
        final var upgrade = new ItemStack(ModItems.NAVIGATION_UPGRADE.get());
        final var origin = helper.absolutePos(new BlockPos(1, 1, 1));
        NavigationUpgradeItem.storeMapData(upgrade, new NavigationMapData(origin.getX(), origin.getZ(), 2, -1));
        restore(helper, upgrade, """
            local upgrade = component.proxy(component.list('navigation')())
            local range = upgrade.getRange()
            local x, y, z = upgrade.getPosition()
            assert(x and y and z, 'navigation map does not cover robot')
            """, """
            assert(upgrade.getRange() == range, 'navigation map scale lost')
            local nx, ny, nz = upgrade.getPosition()
            assert(nx == x and ny == y and nz == z, 'navigation map coordinates changed')
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void robotChunkloaderProxyRetainsActiveState(GameTestHelper helper) {
        restore(helper, new ItemStack(ModItems.CHUNKLOADER_UPGRADE.get()), """
            local upgrade = component.proxy(component.list('chunkloader')())
            upgrade.setActive(true)
            assert(upgrade.isActive())
            """, """
            assert(upgrade.isActive(), 'chunkloader active state lost')
            assert(upgrade.setActive(false), 'old chunkloader proxy failed')
            assert(not upgrade.isActive())
            """);
    }

    private static void restore(GameTestHelper helper, ItemStack upgrade, String setup, String verify) {
        final boolean chunkloader = upgrade.is(ModItems.CHUNKLOADER_UPGRADE.get());
        final boolean generator = upgrade.is(ModItems.GENERATOR_UPGRADE.get());
        final boolean tank = upgrade.is(ModItems.TANK_UPGRADE.get());
        final boolean tractor = upgrade.is(ModItems.TRACTOR_BEAM_UPGRADE.get());
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity original = helper.getBlockEntity(pos);
        original.setTier(2);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        RobotMovementPersistenceGameTests.installHardware(helper, original, List.of(cpu,
            new ItemStack(ModItems.MEMORY_TIER2.get()), upgrade,
            RobotMovementPersistenceGameTests.eeprom("""
                local robot = component.proxy(component.list('robot')())
                assert(robot.getLightColor() ~= 0x731, 'unexpected reboot')
                """ + setup + """
                robot.setLightColor(0x731)
                repeat until computer.pullSignal() == 'continue_upgrade'
                """ + verify + """
                robot.setLightColor(0x123456)
                while true do computer.pullSignal() end
                """)));
        original.onLoad();
        if (upgrade.is(ModItems.INVENTORY_CONTROLLER_UPGRADE.get())) {
            original.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        }
        if (upgrade.is(ModItems.TANK_CONTROLLER_UPGRADE.get())) {
            original.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        }
        if (tank) {
            final var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
            helper.assertTrue(original.tank().getFluidTank(0).fill(water,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "Tank fixture rejected water");
        }
        if (generator) original.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(net.minecraft.world.item.Items.COAL, 3));
        helper.assertTrue(original.toggleMachine(), "Upgrade test robot did not start");
        final RobotBlockEntity[] active = {original};
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(color(original) == 0x731, "Upgrade setup failed: " + original.machine().lastError()))
            .thenExecute(() -> {
                if (chunkloader) assertTickets(helper, original, true);
                final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
                original.setRemoved();
                if (chunkloader) assertTickets(helper, original, false);
                active[0] = (RobotBlockEntity) BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(active[0]);
                active[0].onLoad();
            })
            .thenWaitUntil(() -> {
                if (chunkloader) assertTickets(helper, active[0], true);
            })
            .thenExecute(() -> {
                if (tractor) {
                    final var robot = active[0];
                    final var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), robot.xPosition(), robot.yPosition() + 0.5,
                        robot.zPosition(), new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                    item.setNoPickUpDelay();
                    helper.getLevel().addFreshEntity(item);
                }
            })
            .thenExecute(() -> helper.assertTrue(active[0].machine().signal("continue_upgrade"), "Restored robot rejected signal"))
            .thenWaitUntil(() -> helper.assertTrue(color(active[0]) == 0x123456, "Upgrade continuation failed: " + active[0].machine().lastError()))
            .thenExecute(() -> {
                if (chunkloader) assertTickets(helper, active[0], false);
                if (tractor) {
                    final var cargo = active[0].getItem(RobotBlockEntity.CARGO_SLOT_START);
                    helper.assertTrue(cargo.is(net.minecraft.world.item.Items.DIAMOND) && cargo.getCount() == 3, "Tractor pickup lost items");
                }
                if (generator) {
                    helper.assertTrue(droppedCoal(helper, active[0]) == 0, "Reload duplicated generator fuel as drops");
                    helper.assertTrue(active[0].getItem(RobotBlockEntity.CARGO_SLOT_START).isEmpty(), "Reload duplicated generator fuel in cargo");
                }
            })
            .thenSucceed();
    }

    private static int color(RobotBlockEntity robot) { return (Integer) robot.getLightColor(null, null)[0]; }

    private static void assertTickets(GameTestHelper helper, RobotBlockEntity robot, boolean expected) {
        NeoOpenComputersGameTests.assertModForcedTickingChunksAround(helper,
            robot.getBlockPos(), expected);
    }
}
