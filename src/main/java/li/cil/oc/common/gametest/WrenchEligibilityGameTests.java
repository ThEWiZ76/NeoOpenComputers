package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.WrenchTools;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class WrenchEligibilityGameTests {
    @GameTest(template = "empty")
    public static void defaultWrenchUsageIsIndependentOfRotation(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var tool = new ItemStack(ModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        final var adapterPos = helper.absolutePos(new BlockPos(1, 1, 1));
        final var chargerPos = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.getLevel().setBlockAndUpdate(adapterPos, ModBlocks.ADAPTER.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(chargerPos, ModBlocks.CHARGER.get().defaultBlockState());
        final var adapter = (AdapterBlockEntity) helper.getLevel().getBlockEntity(adapterPos);
        final var charger = (ChargerBlockEntity) helper.getLevel().getBlockEntity(chargerPos);
        final var originalState = charger.getBlockState();
        helper.assertTrue(WrenchTools.holdsApplicableWrench(player, adapterPos), "Default wrench eligibility incorrectly requires block rotation");
        helper.assertTrue(WrenchTools.holdsApplicableWrench(player, chargerPos) && charger.getBlockState().equals(originalState), "Wrench eligibility query mutated block");
        WrenchTools.wrenchUsed(player, chargerPos);
        helper.assertTrue(charger.getBlockState().equals(originalState) && !charger.invertSignal(), "Usage notification performed a block interaction");
        helper.assertTrue(adapter.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(adapterPos), Direction.EAST, adapterPos, false)).consumesAction()
            && !adapter.isSideOpen(Direction.EAST) && adapter.isSideOpen(Direction.WEST), "Default wrench failed side-specific adapter action");
        helper.assertTrue(charger.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(chargerPos), Direction.EAST, chargerPos, false)).consumesAction()
            && charger.invertSignal() && charger.getBlockState().equals(originalState), "Default wrench changed charger rotation instead of only its signal mode");
        helper.succeed();
    }
}
