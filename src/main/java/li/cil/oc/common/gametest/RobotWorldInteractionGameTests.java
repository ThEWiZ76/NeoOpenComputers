package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RobotWorldInteractionGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void robotLuaResumesAfterBlockDigCompletes(final GameTestHelper helper) {
        final RobotBlockEntity robot = robot(helper, """
            local robot = component.proxy(component.list('robot')())
            assert(robot.compare(3), 'compare did not see selected stone')
            assert(robot.swing(3))
            assert(not robot.detect(3), 'swing returned before block was removed')
            robot.setLightColor(0x349ABC)
            while true do computer.pullSignal() end
            """);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.STONE));
        helper.succeedWhen(() -> helper.assertTrue(robot.lightColor() == 0x349ABC,
            "Lua did not resume after dig completion: " + robot.machine().lastError()));
    }

    @GameTest(template = "empty")
    public static void robotDigTimeUsesToolSpeedAndConfiguredRatio(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final double ratio = ModSettings.robotHarvestRatio();
        try {
            ModSettings.ROBOT_HARVEST_RATIO.set(2D);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Ratio dig did not start");
        } finally {
            ModSettings.ROBOT_HARVEST_RATIO.set(ratio);
        }
        // Stone hardness 1.5, iron pick speed 6: 1.5 * 1.5 / 6 * 2 = .75 seconds.
        tickRobot(robot, 14);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Dig ignored configured ratio or tool speed");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Dig did not finish after calculated fifteen ticks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingWaitsForAdjustedDigTime(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final Object timing = new Object() {
            @SubscribeEvent
            public void onDig(final li.cil.oc.api.event.RobotBreakBlockEvent.Pre event) {
                if (event.agent == robot) event.setBreakTime(0.5D);
            }
        };
        NeoForge.EVENT_BUS.register(timing);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", robot.machine(), 3)[0]), "Timed swing did not start");
        } finally {
            NeoForge.EVENT_BUS.unregister(timing);
        }
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Robot broke block before dig time elapsed");
        helper.assertTrue(robot.machine().isPaused(), "Dig did not pause calling computer");
        tickRobot(robot, 9);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Robot completed dig too early");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Robot did not finish ten-tick dig");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingCancelsWhenTargetChangesOrMachineStops(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final var component = (li.cil.oc.api.network.Component) robot.node();
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Dig did not start");
        helper.setBlock(target, Blocks.DIAMOND_BLOCK);
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_BLOCK), "Dig destroyed replacement block");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Canceled dig wore tool");
        helper.setBlock(target, Blocks.STONE);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Second dig did not start");
        robot.machine().stop();
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Stopped robot continued digging");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Stopped robot wore tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingUsesEquippedToolAndHarvestsOnce(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        final ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(7);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIRT, 4));

        final double previousRate = ModSettings.robotItemDamageRate();
        final Object[] result;
        try {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(1D);
            result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
            tickRobot(robot, 100);
        } finally {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(previousRate);
        }
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && helper.getBlockState(target).isAir(), "Equipped pickaxe did not break stone");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 8, "Swing did not damage equipped tool exactly once");
        helper.assertTrue(robot.getItem(RobotBlockEntity.CARGO_SLOT_START).getCount() == 4, "Swing changed selected cargo");
        helper.assertTrue(result.length == 2 && "block".equals(result[1]), "Successful swing must identify block interaction");
        final long drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(helper.absolutePos(target)).inflate(1D)).stream()
            .filter(entity -> entity.getItem().is(Items.COBBLESTONE)).mapToLong(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(drops == 1, "Swing lost or duplicated harvested stone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingHonorsConfiguredAndEventAdjustedWear(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        final double previousRate = ModSettings.robotItemDamageRate();
        final boolean[] observedDamage = {false};
        final Object modifier = new Object() {
            @SubscribeEvent
            public void onDamage(final RobotUsedToolEvent.ComputeDamageRate event) {
                if (event.agent != robot) return;
                helper.assertTrue(event.toolBeforeUse.getDamageValue() == 7 && event.toolAfterUse.getDamageValue() == 8,
                    "Damage modifier did not receive actual tool wear");
                helper.assertTrue(event.getDamageRate() == 0.1D, "Damage modifier did not receive configured base rate");
                observedDamage[0] = true;
                event.setDamageRate(0D);
            }
        };
        try {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(0D);
            helper.setBlock(target, Blocks.STONE);
            final ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
            tool.setDamageValue(7);
            robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Zero-wear robot could not harvest");
            tickRobot(robot, 100);
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Zero configured rate still damaged tool");

            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(0.1D);
            helper.setBlock(target, Blocks.STONE);
            NeoForge.EVENT_BUS.register(modifier);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Modified-wear robot could not harvest");
            tickRobot(robot, 100);
            helper.assertTrue(observedDamage[0], "Swing skipped damage modifier");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Swing ignored modified damage rate");
        } finally {
            NeoForge.EVENT_BUS.unregister(modifier);
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(previousRate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRespectsHarvestToolAndBlockProtection(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.DIAMOND_ORE);
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(Boolean.FALSE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Robot harvested ore using a cargo tool instead of equipped tool");
        helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_ORE), "Failed harvest removed ore");
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean[] protectionCalled = {false};
        final Object protection = new Object() {
            @SubscribeEvent
            public void onBreak(final BlockEvent.BreakEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    protectionCalled[0] = true;
                    event.setCanceled(true);
                }
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Robot did not start protected-block dig");
            tickRobot(robot, 100);
            helper.assertTrue(protectionCalled[0], "Robot skipped standard block protection");
            helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_ORE), "Protected ore was removed");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Blocked swing damaged tool");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    static RobotBlockEntity robot(final GameTestHelper helper) {
        return robot(helper, "while true do computer.pullSignal() end");
    }

    private static RobotBlockEntity robot(final GameTestHelper helper, final String code) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.SOUTH));
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(
            new ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get()), new ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()),
            RobotMovementPersistenceGameTests.eeprom(code)));
        robot.onLoad();
        ((li.cil.oc.api.network.Connector) robot.machine().node()).changeBuffer(10000D);
        helper.assertTrue(robot.toggleMachine(), "Interaction fixture did not start");
        return robot;
    }

    static void tickRobot(final RobotBlockEntity robot, final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            RobotBlockEntity.serverTick(robot.getLevel(), robot.getBlockPos(), robot.getBlockState(), robot);
        }
    }
}
