package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RobotControlGameTests {
    @GameTest(template = "empty")
    public static void robotNameCallbacksPreserveExactNameInWorldItemAndClientData(final GameTestHelper helper) throws Exception {
        final var robot = fixture(helper, "while true do computer.pullSignal() end");
        final var component = (Component) robot.node();
        helper.assertTrue(component.methods().containsAll(List.of("getName", "setName")), "Robot lacks upstream name callbacks");
        final String renamed = "Port robot \uD83D\uDC22";
        helper.assertTrue(Arrays.equals(component.invoke("setName", null, renamed), new Object[]{"Robot"}), "Rename did not return old name");
        helper.assertTrue(Arrays.equals(component.invoke("getName", null), new Object[]{renamed})
            && Arrays.equals(component.invoke("name", null), new Object[]{renamed}), "Name callbacks disagree");
        final var update = robot.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(update.getString("oc:name").equals(renamed), "Client update omitted robot name");
        final var mirror = new RobotBlockEntity(robot.getBlockPos(), robot.getBlockState());
        try {
            mirror.handleUpdateTag(update, helper.getLevel().registryAccess());
            helper.assertTrue(mirror.name().equals(renamed), "Client did not apply robot name");
        } finally { mirror.setRemoved(); }
        final var item = robot.createRobotDrop(helper.getLevel().registryAccess());
        helper.assertTrue(item.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getString("oc:name").equals(renamed),
            "Assembled item omitted renamed robot");
        assertRestoredName(helper, robot, renamed);
        helper.assertTrue(Arrays.equals(component.invoke("setName", null, ""), new Object[]{renamed}), "Empty rename lost old name result");
        helper.assertTrue(robot.name().isEmpty(), "Empty name was changed despite upstream accepting it");
        helper.assertTrue(Arrays.equals(component.invoke("setName", null, "  "), new Object[]{""}), "Whitespace rename lost old name result");
        assertRestoredName(helper, robot, "  ");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeRobotControlCallbacksRespectRunningAndPausedState(final GameTestHelper helper) throws Exception {
        final var robot = fixture(helper, """
            local r = component.proxy(component.list('robot')())
            assert(r.getName() == 'Port Robot', 'wrong robot name')
            local old, reason = r.setName('forbidden')
            assert(old == nil and reason == 'is running', 'running rename was accepted')
            assert(r.isRunning(), 'robot reports stopped')
            component.proxy(component.list('eeprom')()).setData('checked')
            while true do computer.pullSignal() end
            """);
        robot.setName("Port Robot");
        final var component = (Component) robot.node();
        helper.assertTrue(component.methods().containsAll(List.of("start", "stop", "isRunning")), "Robot lacks upstream control callbacks");
        helper.assertTrue(Arrays.equals(component.invoke("isRunning", null), new Object[]{false}), "New robot reports running");
        helper.assertTrue(Arrays.equals(component.invoke("start", null), new Object[]{true}), "Robot callback failed to start native VM");
        helper.assertTrue(Arrays.equals(component.invoke("start", null), new Object[]{false}), "Repeated start changed state");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(robot).equals("checked"),
                "Native robot did not exercise control/name callbacks: " + robot.machine().lastError()))
            .thenExecute(() -> {
                try {
                    helper.assertTrue(robot.machine().pause(1000), "Could not pause robot fixture");
                    helper.assertTrue(Arrays.equals(component.invoke("start", null), new Object[]{false}) && robot.machine().isPaused(),
                        "Robot start resumed a paused machine");
                    helper.assertTrue(Arrays.equals(component.invoke("setName", null, "paused rename"), new Object[]{null, "is running"})
                        && robot.name().equals("Port Robot"), "Paused robot was renamed");
                    helper.assertTrue(Arrays.equals(component.invoke("stop", null), new Object[]{true}) && !robot.machine().isRunning(),
                        "Robot callback failed to stop");
                    helper.assertTrue(Arrays.equals(component.invoke("stop", null), new Object[]{false}), "Repeated stop changed state");
                    helper.assertTrue(Arrays.equals(component.invoke("setName", null, "Stopped Robot"), new Object[]{"Port Robot"}),
                        "Stopped robot could not be renamed");
                } catch (Exception exception) { throw new IllegalStateException(exception); }
            }).thenSucceed();
    }

    private static RobotBlockEntity fixture(final GameTestHelper helper, final String program) {
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, List.of(cpu,
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom(program)));
        robot.onLoad();
        ((Connector) robot.machine().node()).changeBuffer(10000);
        return robot;
    }

    private static void assertRestoredName(final GameTestHelper helper, final RobotBlockEntity robot, final String expected) {
        final var saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var restored = (RobotBlockEntity) BlockEntity.loadStatic(robot.getBlockPos(), robot.getBlockState(), saved,
            helper.getLevel().registryAccess());
        helper.assertTrue(restored != null, "Could not restore robot");
        try {
            helper.assertTrue(restored.name().equals(expected), "Robot world load changed saved name");
        } finally { restored.setRemoved(); }
    }

    private static String marker(final RobotBlockEntity robot) {
        for (final var stack : robot.internalComponents()) {
            if (stack.is(ModItems.EEPROM.get())) return new String(ItemDriverData.dataTag(stack)
                .getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
        }
        return "";
    }
}
