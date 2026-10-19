package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.WrenchTools;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ProjectRedWrenchGameTests {
    @GameTest(template = "empty")
    public static void projectRedScrewdriverTogglesAdapterAndConsumesDurability(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("projectred_core")) {
            helper.assertTrue(!Boolean.getBoolean("neoopencomputers.testProjectRedCore"), "Requested ProjectRed Core was not loaded");
            helper.succeed();
            return;
        }
        final var screwdriver = new ItemStack(BuiltInRegistries.ITEM.getOptional(
            ResourceLocation.fromNamespaceAndPath("projectred_core", "screwdriver")).orElseThrow());
        helper.assertTrue(WrenchTools.isWrench(screwdriver), "Real ProjectRed screwdriver is not registered as an OC wrench");
        final var config = Class.forName("mrtjp.projectred.core.Configurator").getField("SERVER").get(null);
        final var unbreakable = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue)
            config.getClass().getField("unbreakableScrewdriver").get(config);
        final boolean previous = unbreakable.get();
        unbreakable.set(false);
        try {
            final var player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, screwdriver);
            final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ADAPTER.get().defaultBlockState());
            final var adapter = (AdapterBlockEntity) helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(WrenchTools.holdsApplicableWrench(player, pos) && screwdriver.getDamageValue() == 0,
                "Wrench eligibility check rejected or damaged the ProjectRed screwdriver");
            final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
            helper.assertTrue(adapter.getBlockState().useItemOn(screwdriver, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction()
                && !adapter.isSideOpen(Direction.EAST) && adapter.isSideOpen(Direction.WEST)
                && screwdriver.getDamageValue() == 1, "ProjectRed screwdriver did not close the clicked adapter side and consume one durability");
            player.setShiftKeyDown(true);
            adapter.getBlockState().useItemOn(screwdriver, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(!adapter.isSideOpen(Direction.WEST) && screwdriver.getDamageValue() == 2,
                "Sneaking ProjectRed screwdriver did not close the opposite side and consume exactly one durability");
        } finally { unbreakable.set(previous); }
        helper.succeed();
    }

    private ProjectRedWrenchGameTests() {}
}
