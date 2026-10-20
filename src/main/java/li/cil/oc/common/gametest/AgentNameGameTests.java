package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AgentNameGameTests {
    @GameTest(template = "empty")
    public static void missingAndEmptyWorldNamesRegenerateOnceAndThenPersist(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var drone = new DroneEntity(helper.getLevel());
        try {
            for (final boolean emptyTag : new boolean[]{false, true}) {
                final var robotData = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
                final var droneData = drone.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                robotData.remove("oc:name"); droneData.remove("oc:name");
                if (emptyTag) { robotData.putString("oc:name", ""); droneData.putString("oc:name", ""); }
                robot.loadWithComponents(robotData, helper.getLevel().registryAccess());
                drone.load(droneData);
                final String robotName = robot.name();
                final String droneName = drone.name();
                helper.assertTrue(!robotName.isEmpty() && !robotName.equals("Robot"), "Missing/empty robot name used generic fallback");
                helper.assertTrue(!droneName.isEmpty() && !droneName.equals("Drone") && !droneName.equals("Robot"),
                    "Missing/empty drone name used generic fallback");
                robot.loadWithComponents(robot.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
                drone.load(drone.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
                helper.assertTrue(robot.name().equals(robotName) && drone.name().equals(droneName), "Saving generated name rerolled it");
            }
            drone.setName("  ");
            drone.load(drone.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            helper.assertTrue(drone.name().equals("  "), "Drone world load changed nonempty whitespace name");
        } finally { drone.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerAssignsNamesBeforeRobotAndDronePlacement(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity assembler = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var robotItem = assemble(helper, assembler, new ItemStack(ModItems.COMPUTER_CASE_TIER1.get()));
        helper.assertTrue(robotItem.has(DataComponents.CUSTOM_NAME), "Assembler robot output lacks generated display name");
        final String robotName = robotItem.get(DataComponents.CUSTOM_NAME).getString();
        helper.assertTrue(!robotName.isEmpty(), "Assembler generated an empty robot name");
        final var robot = placeRobot(helper, robotItem, new BlockPos(3, 0, 1));
        helper.assertTrue(robot.name().equals(robotName), "Placement changed assembler robot name");
        final var droneItem = assemble(helper, assembler, new ItemStack(ModItems.DRONE_CASE_TIER1.get()));
        helper.assertTrue(droneItem.has(DataComponents.CUSTOM_NAME), "Assembler drone output lacks generated display name");
        final String droneName = droneItem.get(DataComponents.CUSTOM_NAME).getString();
        final var drone = new DroneEntity(helper.getLevel());
        try {
            drone.loadFromItemStack(droneItem, null);
            helper.assertTrue(!droneName.isEmpty() && drone.name().equals(droneName), "Placement changed assembler drone name");
        } finally { drone.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilRobotNameOverridesStoredNameAndSurvivesDropReload(final GameTestHelper helper) {
        final var item = new ItemStack(ModItems.ROBOT.get());
        final var data = new net.minecraft.nbt.CompoundTag();
        data.putString("oc:name", "Old stored name");
        net.minecraft.world.item.BlockItem.setBlockEntityData(item, li.cil.oc.common.ModBlockEntities.ROBOT.get(), data);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Surveyor \uD83D\uDC22"));
        final var robot = placeRobot(helper, item, new BlockPos(1, 0, 1));
        helper.assertTrue(robot.name().equals("Surveyor \uD83D\uDC22"), "Anvil display name did not override stored robot name");
        final var drop = robot.createRobotDrop(helper.getLevel().registryAccess());
        helper.assertTrue(drop.has(DataComponents.CUSTOM_NAME) && drop.getHoverName().getString().equals(robot.name()),
            "Robot drop lost display name");
        robot.setName("  ");
        final var saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
        robot.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(robot.name().equals("  "), "World load changed nonempty whitespace name");
        helper.succeed();
    }

    private static ItemStack assemble(final GameTestHelper helper, final AssemblerBlockEntity assembler, final ItemStack template) {
        assembler.clearContent();
        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, template);
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, new ItemStack(ModItems.CPU_TIER1.get()));
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 2,
            RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
        helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Assembler rejected name fixture");
        final var result = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).copy();
        assembler.removeItemNoUpdate(AssemblerBlockEntity.SLOT_TEMPLATE);
        return result;
    }

    private static RobotBlockEntity placeRobot(final GameTestHelper helper, final ItemStack stack, final BlockPos support) {
        helper.setBlock(support, Blocks.STONE);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var pos = helper.absolutePos(support);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction() && stack.isEmpty(),
            "Survival fixture could not place/consume robot item");
        final RobotBlockEntity robot = helper.getBlockEntity(support.above());
        robot.onLoad();
        return robot;
    }
}
