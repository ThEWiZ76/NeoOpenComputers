package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ToolDurabilityProviderImc;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.InterModComms;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RobotDurabilityGameTests {
    private static volatile boolean providerActive;

    @GameTest(template = "empty")
    public static void robotDurabilityUsesEquipmentAndRegisteredProviders(GameTestHelper helper) throws Exception {
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var component = (Component) robot.node();
        helper.assertTrue(component.methods().contains("durability"), "Robot component lacks upstream durability callback");
        helper.assertTrue(java.util.Arrays.equals(component.invoke("durability", null), new Object[]{null, "no tool equipped"}), "Missing tool result differs from upstream");
        robot.equipmentInventory().setItem(0, new ItemStack(Items.DIRT));
        helper.assertTrue(java.util.Arrays.equals(component.invoke("durability", null), new Object[]{null, "tool cannot be damaged"}), "Undamageable tool result differs from upstream");
        robot.equipmentInventory().setItem(0, damagedTool());
        helper.assertTrue(java.util.Arrays.equals(component.invoke("durability", null), new Object[]{0.75D}), "Equipped tool fraction is incorrect");
        final var saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
        robot.equipmentInventory().setItem(0, ItemStack.EMPTY);
        robot.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(java.util.Arrays.equals(component.invoke("durability", null), new Object[]{0.75D})
            && robot.equipmentInventory().getItem(0).getDamageValue() == 100, "Tool metadata/read-only durability changed after restoration");
        ToolDurabilityProviderImc.process(java.util.stream.Stream.of(new InterModComms.IMCMessage("gametest", "neoopencomputers",
            li.cil.oc.api.IMC.REGISTER_TOOL_DURABILITY_PROVIDER, () -> RobotDurabilityGameTests.class.getName() + ".customDurability")));
        providerActive = true;
        try {
            robot.equipmentInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
            helper.assertTrue(java.util.Arrays.equals(component.invoke("durability", null), new Object[]{0.375D}), "Robot ignored the registered tool durability provider");
        } finally { providerActive = false; }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nativeRobotProgramCanReadToolDurability(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(cpu,
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom("""
                local r = component.proxy(component.list('robot')())
                assert(r.durability() == 0.75, 'tool durability differs')
                r.setLightColor(0x12AB34)
                while true do computer.pullSignal() end
                """)));
        robot.equipmentInventory().setItem(0, damagedTool());
        robot.onLoad();
        ((Connector) robot.machine().node()).changeBuffer(10000);
        helper.assertTrue(robot.machine().start(), "Native robot did not start");
        helper.succeedWhen(() -> helper.assertTrue(robot.lightColor() == 0x12AB34
            && robot.equipmentInventory().getItem(0).getDamageValue() == 100,
            "Real native program could not read tool durability: " + robot.machine().lastError()));
    }

    private static ItemStack damagedTool() {
        final var tool = new ItemStack(Items.IRON_PICKAXE);
        tool.set(DataComponents.MAX_DAMAGE, 400);
        tool.setDamageValue(100);
        return tool;
    }

    public static double customDurability(ItemStack stack) {
        return providerActive && stack.is(Items.AMETHYST_SHARD) ? 0.375D : Double.NaN;
    }

    private RobotDurabilityGameTests() {}
}
