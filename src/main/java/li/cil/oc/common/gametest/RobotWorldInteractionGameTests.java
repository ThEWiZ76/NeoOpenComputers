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
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Zero configured rate still damaged tool");

            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(0.1D);
            helper.setBlock(target, Blocks.STONE);
            NeoForge.EVENT_BUS.register(modifier);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Modified-wear robot could not harvest");
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
        final Object protection = new Object() {
            @SubscribeEvent
            public void onBreak(final BlockEvent.BreakEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            helper.assertTrue(Boolean.FALSE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Robot bypassed standard block protection");
            helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_ORE), "Protected ore was removed");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Blocked swing damaged tool");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    private static RobotBlockEntity robot(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.SOUTH));
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        return robot;
    }
}
