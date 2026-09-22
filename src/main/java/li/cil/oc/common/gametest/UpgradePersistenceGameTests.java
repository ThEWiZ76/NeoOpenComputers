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
            .thenExecute(() -> helper.assertTrue(active[0].machine().signal("continue_upgrade"), "Restored robot rejected signal"))
            .thenWaitUntil(() -> helper.assertTrue(color(active[0]) == 0x123456, "Upgrade continuation failed: " + active[0].machine().lastError()))
            .thenExecute(() -> {
                if (chunkloader) assertTickets(helper, active[0], false);
            })
            .thenSucceed();
    }

    private static int color(RobotBlockEntity robot) { return (Integer) robot.getLightColor(null, null)[0]; }

    private static void assertTickets(GameTestHelper helper, RobotBlockEntity robot, boolean expected) {
        NeoOpenComputersGameTests.assertModForcedTickingChunksAround(helper,
            new net.minecraft.world.level.ChunkPos(robot.getBlockPos()), expected);
    }
}
