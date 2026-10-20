package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class EnderIOGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("enderio")) {
            if (Boolean.getBoolean("neoopencomputers.testEnderIO")) throw new IllegalStateException("Requested Ender IO fixture is absent");
            return List.of();
        }
        return List.of(
            new TestFunction("enderio", "enderio_actual_yeta_wrench_oc_actions", "neoopencomputers:ae2_export", 200, 0, true, Loaded::actions),
            new TestFunction("enderio", "enderio_actual_yeta_wrench_drone_pickup", "neoopencomputers:ae2_export", 200, 0, true, Loaded::dronePickup));
    }

    private static final class Loaded {
        private static ItemStack wrench() {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("enderio", "yeta_wrench")));
        }

        static void actions(GameTestHelper helper) {
            final var stack = wrench();
            helper.assertTrue(!stack.isEmpty() && stack.getItem() instanceof com.enderio.enderio.content.tools.YetaWrenchItem, "Actual published Yeta Wrench is missing");
            final var player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            final var adapterPos = helper.absolutePos(new BlockPos(2, 2, 2));
            final var chargerPos = adapterPos.east(2);
            helper.getLevel().setBlockAndUpdate(adapterPos, ModBlocks.ADAPTER.get().defaultBlockState());
            helper.getLevel().setBlockAndUpdate(chargerPos, ModBlocks.CHARGER.get().defaultBlockState());
            final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) helper.getLevel().getBlockEntity(adapterPos);
            final var charger = (li.cil.oc.common.blockentity.ChargerBlockEntity) helper.getLevel().getBlockEntity(chargerPos);
            final var original = stack.copy();
            final boolean open = adapter.isSideOpen(Direction.EAST), inverted = charger.invertSignal();
            for (final var pos : List.of(adapterPos, chargerPos)) {
                final var context = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false));
                helper.assertTrue(stack.getItem().onItemUseFirst(stack, context) == net.minecraft.world.InteractionResult.PASS
                    && ItemStack.matches(stack, original), "Ender IO item-first handling intercepts an OC host or changes the wrench");
            }
            helper.assertTrue(WrenchTools.isWrench(stack), "Actual Ender IO Yeta Wrench is not registered with OC");
            helper.assertTrue(WrenchTools.holdsApplicableWrench(player, adapterPos) && WrenchTools.holdsApplicableWrench(player, chargerPos)
                && adapter.isSideOpen(Direction.EAST) == open && charger.invertSignal() == inverted && ItemStack.matches(stack, original), "Wrench eligibility mutated state");
            WrenchTools.wrenchUsed(player, chargerPos);
            helper.assertTrue(charger.invertSignal() == inverted && ItemStack.matches(stack, original), "Usage notification repeated an action or consumed the wrench");
            helper.assertTrue(adapter.getBlockState().useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(adapterPos), Direction.EAST, adapterPos, false)).consumesAction()
                && adapter.isSideOpen(Direction.EAST) != open && adapter.isSideOpen(Direction.WEST), "Yeta Wrench did not toggle only the selected adapter side");
            helper.assertTrue(charger.getBlockState().useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(chargerPos), Direction.EAST, chargerPos, false)).consumesAction()
                && charger.invertSignal() != inverted && ItemStack.matches(stack, original), "Yeta Wrench failed signal toggle or changed stack data");
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            final boolean after = adapter.isSideOpen(Direction.EAST);
            adapter.getBlockState().useItemOn(stack, helper.getLevel(), player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(adapterPos), Direction.EAST, adapterPos, false));
            helper.assertTrue(adapter.isSideOpen(Direction.EAST) == after && !WrenchTools.isWrench(player.getMainHandItem())
                && !WrenchTools.holdsApplicableWrench(player, adapterPos), "Offhand or ordinary item bypassed main-hand wrench eligibility");
            helper.succeed();
        }

        static void dronePickup(GameTestHelper helper) {
            final var stack = wrench();
            final var original = stack.copy();
            final var pos = helper.absolutePos(new BlockPos(3, 3, 3));
            final var drone = new li.cil.oc.common.entity.DroneEntity(helper.getLevel());
            drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            drone.loadFromItemStack(((li.cil.oc.common.item.DroneItem) ModItems.DRONE.get()).assembleFromCase(new ItemStack(ModItems.DRONE_CASE_TIER1.get())), null);
            helper.getLevel().addFreshEntity(drone);
            try {
                final var player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setShiftKeyDown(true);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                drone.interact(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(drone.isRemoved() && ItemStack.matches(stack, original), "Yeta Wrench failed type-only drone pickup or changed the tool");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1))
                    .stream().filter(entity -> entity.getItem().is(ModItems.DRONE.get())).mapToInt(entity -> entity.getItem().getCount()).sum() == 1,
                    "Yeta Wrench drone pickup lost or duplicated packed item");
            } finally { drone.discard(); }
            helper.succeed();
        }
    }
}
