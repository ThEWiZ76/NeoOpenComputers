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
    @GameTest(template = "empty")
    public static void robotSwingUsesCalibratedRayForPartialBlocks(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE_SLAB);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final var component = (li.cil.oc.api.network.Component) robot.node();
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("swing", null, 3, 1)[0]), "Upward calibrated ray hit empty half above bottom slab");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3, 0)[0]), "Downward calibrated ray missed bottom slab");
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Calibrated slab dig did not finish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingHonorsSeparateBlockAndItemClickDenials(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.REDSTONE_ORE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final int[] mode = {0};
        final boolean[] sneakyHarvest = {false};
        final Object listener = new Object() {
            @SubscribeEvent
            public void onClick(final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
                if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(target))) return;
                if (mode[0] == 0) event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
                if (mode[0] == 1) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
            }
            @SubscribeEvent
            public void onHarvest(final BlockEvent.BreakEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    sneakyHarvest[0] = event.getPlayer().isShiftKeyDown();
                }
            }
        };
        final var component = (li.cil.oc.api.network.Component) robot.node();
        NeoForge.EVENT_BUS.register(listener);
        try {
            helper.assertTrue(Boolean.FALSE.equals(component.invoke("swing", null, 3)[0]), "Use-item denial started a dig");
            helper.assertTrue(!helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Denied click still attacked ore");
            mode[0] = 1;
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3, 3, true)[0]), "Use-block denial incorrectly blocked item mining");
            helper.assertTrue(!helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Use-block denial still attacked ore");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).isAir() && sneakyHarvest[0], "Delayed harvest lost sneak state or did not finish");
            helper.assertTrue(!robot.player().isShiftKeyDown(), "Harvest leaked sneak state");
            mode[0] = 2;
            helper.setBlock(target, Blocks.REDSTONE_ORE);
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Normal ore click failed");
            helper.assertTrue(helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Normal click omitted block attack behavior");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).isAir(), "Block attack state change canceled valid dig");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRejectsUnsupportedSidesAndOppositeCalibration(final GameTestHelper helper) throws Exception {
        final var component = (li.cil.oc.api.network.Component) robot(helper).node();
        for (final Object[] arguments : new Object[][]{{2}, {4}, {5}, {-1}, {6}, {3, 2}, {0, 1}, {1, 0}, {3, 6}}) {
            try {
                component.invoke("swing", null, arguments);
                helper.fail("Swing accepted invalid side arguments " + java.util.Arrays.toString(arguments));
            } catch (final IllegalArgumentException expected) {
                helper.assertTrue("invalid side".equals(expected.getMessage()), "Unexpected side validation error");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRespectsLeftClickProtectionAndSneaking(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean[] observed = {false};
        final Object protection = new Object() {
            @SubscribeEvent
            public void onClick(final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
                if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(target))) return;
                observed[0] = true;
                helper.assertTrue(event.getEntity().isShiftKeyDown(), "Robot omitted sneak modifier from left click");
                helper.assertTrue(event.getEntity().getMainHandItem().is(Items.IRON_PICKAXE), "Left click did not use equipped tool");
                helper.assertTrue(event.getFace() == Direction.NORTH, "Left click did not use actual hit face");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3, 1, true);
            helper.assertTrue(observed[0], "Robot skipped left-click event");
            helper.assertTrue(Boolean.FALSE.equals(result[0]), "Robot ignored canceled left click");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Canceled left click still broke block");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Canceled left click wore tool");
            helper.assertTrue(!robot.player().isShiftKeyDown(), "Robot leaked sneak state into next interaction");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotDigSynchronizesSwingAnimationAndCancellation(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final Object timing = new Object() {
            @SubscribeEvent
            public void onDig(final li.cil.oc.api.event.RobotBreakBlockEvent.Pre event) {
                if (event.agent == robot) event.setBreakTime(1D);
            }
        };
        NeoForge.EVENT_BUS.register(timing);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Animated dig did not start");
        } finally {
            NeoForge.EVENT_BUS.unregister(timing);
        }
        final var registries = helper.getLevel().registryAccess();
        final var packet = robot.getUpdateTag(registries);
        final var animation = packet.getCompound("oc:animation");
        helper.assertTrue(animation.getBoolean("swing") && animation.getInt("ticks") == 20, "Dig did not synchronize swing and duration");
        final RobotBlockEntity client = new RobotBlockEntity(robot.getBlockPos(), robot.getBlockState());
        client.handleUpdateTag(packet, registries);
        helper.assertTrue(client.getItem(RobotBlockEntity.TOOL_SLOT).is(Items.IRON_PICKAXE), "Swing packet lost equipped tool");
        final long start = animation.getLong("start");
        helper.assertTrue(Math.abs(client.swingRenderOffset(start)) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 5D) - 45F) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 10D)) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 15D) - 45F) < 1e-5
            && client.swingRenderOffset(start + 20D) == 0F, "Swing did not follow upstream repeated arc");
        client.handleUpdateTag(packet, registries);
        helper.assertTrue(Math.abs(client.swingRenderOffset(start + 15D) - 45F) < 1e-5, "Repeated packet restarted swing");
        robot.machine().stop();
        tickRobot(robot, 1);
        client.handleUpdateTag(robot.getUpdateTag(registries), registries);
        helper.assertTrue(!client.getUpdateTag(registries).getCompound("oc:animation").getBoolean("swing"), "Canceled dig retained client swing animation");
        helper.assertTrue(client.swingRenderOffset(start + 5D) == 0F, "Canceled swing still renders");

        helper.assertTrue(robot.toggleMachine(), "Short-swing fixture did not restart");
        final double ratio = ModSettings.robotHarvestRatio();
        try {
            ModSettings.ROBOT_HARVEST_RATIO.set(0D);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Short dig did not start");
        } finally {
            ModSettings.ROBOT_HARVEST_RATIO.set(ratio);
        }
        final var shortAnimation = robot.getUpdateTag(registries).getCompound("oc:animation");
        helper.assertTrue(shortAnimation.getInt("ticks") == 5, "Short swing lost upstream minimum duration");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Short dig did not complete");
        client.handleUpdateTag(robot.getUpdateTag(registries), registries);
        helper.assertTrue(Math.abs(client.swingRenderOffset(shortAnimation.getLong("start") + 2.5D) - 45F) < 1e-5,
            "Successful short dig ended swing before five ticks");
        helper.succeed();
    }

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
