package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.TankUpgradeEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TankDataPersistenceGameTests {
    @GameTest(template = "empty")
    public static void robotTankPreservesFluidComponentsOnReload(GameTestHelper helper) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.TANK_UPGRADE.get())));
        robot.onLoad();
        final var fluid = new FluidStack(Fluids.WATER, 731);
        final var metadata = new CompoundTag();
        metadata.putString("batch", "test-mixture");
        metadata.putInt("concentration", 37);
        fluid.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata));
        helper.assertTrue(robot.tank().getFluidTank(0).fill(fluid, FluidAction.EXECUTE) == 731, "Fixture rejected tagged fluid");
        final var saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
        robot.setRemoved();
        final var restored = (RobotBlockEntity) BlockEntity.loadStatic(robot.getBlockPos(), robot.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored);
        restored.onLoad();
        final var actual = restored.tank().getFluidTank(0).getFluid();
        helper.assertTrue(actual.getAmount() == 731 && FluidStack.isSameFluidSameComponents(actual, fluid), "Robot reload lost fluid components");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankMigratesLegacyAndClearsReusedEnvironment(GameTestHelper helper) {
        final var environment = new TankUpgradeEnvironment(null);
        final var legacy = new CompoundTag();
        legacy.putString("fluid", "minecraft:water");
        legacy.putInt("amount", 231);
        environment.load(legacy);
        helper.assertTrue(environment.getFluidAmount() == 231 && environment.getFluid().is(Fluids.WATER), "Legacy tank fluid lost");
        final var saved = new CompoundTag();
        environment.save(saved);
        environment.load(new CompoundTag());
        helper.assertTrue(environment.getFluid().isEmpty(), "Loading empty tank retained previous fluid");
        environment.load(saved);
        helper.assertTrue(environment.getFluidAmount() == 231, "Migrated tank did not reload");
        environment.drain(231, FluidAction.EXECUTE);
        environment.save(saved);
        environment.load(saved);
        helper.assertTrue(environment.getFluid().isEmpty(), "Reused snapshot resurrected drained fluid");
        environment.node().remove();
        helper.succeed();
    }
}
