package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class CableGameTests {
    @GameTest(template = "empty")
    public static void cableDyeInteractionConsumesOnlySurvivalDye(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var cable = place(helper, pos);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var dye = new ItemStack(Items.RED_DYE, 3);
        player.setItemInHand(InteractionHand.OFF_HAND, dye);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        final var result = cable.getBlockState().useItemOn(dye, helper.getLevel(), player, InteractionHand.OFF_HAND, hit);
        helper.assertTrue(result.consumesAction(), "Cable dye interaction was not handled");
        helper.assertTrue(cable.getColor() == DyeColor.RED.getTextureDiffuseColor() && dye.getCount() == 2,
            "Survival recoloring did not apply red and consume exactly one dye");
        cable.getBlockState().useItemOn(dye, helper.getLevel(), player, InteractionHand.OFF_HAND, hit);
        helper.assertTrue(dye.getCount() == 1, "Repeated same-color dye did not follow upstream consumption");
        final var creative = helper.makeMockPlayer(GameType.CREATIVE);
        // GameTest's mock overrides isCreative but does not initialize the corresponding abilities.
        GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
        final var blue = new ItemStack(Items.BLUE_DYE, 3);
        creative.setItemInHand(InteractionHand.MAIN_HAND, blue);
        cable.getBlockState().useItemOn(blue, helper.getLevel(), creative, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(cable.getColor() == DyeColor.BLUE.getTextureDiffuseColor() && blue.getCount() == 3,
            "Creative recoloring consumed dye or failed to set color");
        final var stick = new ItemStack(Items.STICK);
        helper.assertTrue(!cable.getBlockState().useItemOn(stick, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction()
            && cable.getColor() == DyeColor.BLUE.getTextureDiffuseColor(), "Non-dye item changed cable color");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableItemPlacementAndDropsPreserveColor(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var stack = new ItemStack(ModItems.CABLE.get(), 3);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3478AB, true));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        final var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        helper.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(), "Colored cable placement failed");
        final var cable = (CableBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(cable != null && cable.getColor() == 0x3478AB && stack.getCount() == 2,
            "BlockItem placement lost color or consumed the wrong count");
        final var picked = cable.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), pos, cable.getBlockState());
        helper.assertTrue(picked.is(ModItems.CABLE.get()) && picked.getCount() == 1
            && picked.get(DataComponents.DYED_COLOR) != null && picked.get(DataComponents.DYED_COLOR).rgb() == 0x3478AB,
            "Pick-block lost cable color");
        final var drops = Block.getDrops(cable.getBlockState(), helper.getLevel(), pos, cable);
        helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(picked, drops.getFirst())
            && drops.getFirst().getCount() == 1, "Cable loot did not preserve exactly one colored item");
        cable.setColor(CableBlockEntity.DEFAULT_COLOR);
        final var plainDrops = Block.getDrops(cable.getBlockState(), helper.getLevel(), pos, cable);
        helper.assertTrue(plainDrops.size() == 1 && !plainDrops.getFirst().has(DataComponents.DYED_COLOR),
            "Default cable did not stack with ordinary undyed cable items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableColorReloadAndIsolation(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var center = place(helper, pos);
        helper.assertTrue(center instanceof Colored, "Cable does not expose its stored color");
        for (final var side : Direction.values()) {
            ((Colored) place(helper, pos.relative(side))).setColor(DyeColor.BLUE.getTextureDiffuseColor());
        }
        ((Colored) center).setColor(DyeColor.RED.getTextureDiffuseColor());
        helper.assertTrue(center.node().network() != null, "Fully isolated colored cable has no network");
        for (final var side : Direction.values()) {
            final var neighbor = (CableBlockEntity) helper.getLevel().getBlockEntity(pos.relative(side));
            helper.assertTrue(center.node().network() != neighbor.node().network(), "Color mismatch connected on " + side);
        }
        final var loaded = new CableBlockEntity(pos, center.getBlockState());
        final var legacy = new CompoundTag();
        legacy.putInt("oc:renderColor", DyeColor.GREEN.getId());
        loaded.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.GREEN.getTextureDiffuseColor(), "Legacy dye metadata did not migrate");
        legacy.putInt("oc:renderColorRGB", DyeColor.YELLOW.getTextureDiffuseColor());
        loaded.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.YELLOW.getTextureDiffuseColor(), "RGB did not override legacy color");
        loaded.handleUpdateTag(center.getUpdateTag(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == DyeColor.RED.getTextureDiffuseColor(), "Client update lost cable color");
        helper.assertTrue(!center.getUpdateTag(helper.getLevel().registryAccess()).contains("node"), "Client color update leaked server network data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableColorsSplitAndReconnectNetworks(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var first = place(helper, pos);
        final var middle = place(helper, pos.relative(Direction.EAST));
        final var last = place(helper, pos.relative(Direction.EAST, 2));
        helper.assertTrue(first instanceof Colored, "Cable does not implement color connectivity");
        final var firstColor = (Colored) first;
        final var middleColor = (Colored) middle;
        final var lastColor = (Colored) last;
        firstColor.setColor(DyeColor.RED.getTextureDiffuseColor());
        lastColor.setColor(DyeColor.BLUE.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == last.node().network(), "Default light gray cable did not bridge different colors");
        final var address = middle.node().address();
        middleColor.setColor(DyeColor.RED.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == middle.node().network(), "Matching cable colors did not connect");
        helper.assertTrue(first.node().network() != last.node().network(), "Different cable colors remained connected after recoloring");
        Network.joinOrCreateNetwork(helper.getLevel(), last.getBlockPos());
        helper.assertTrue(first.node().network() != last.node().network(), "Joining from the opposite cable bypassed color rules");
        middleColor.setColor(DyeColor.LIGHT_GRAY.getTextureDiffuseColor());
        helper.assertTrue(first.node().network() == last.node().network(), "Restoring wildcard color did not reconnect both networks");
        helper.assertTrue(address.equals(middle.node().address()), "Recoloring changed the cable node address");
        middleColor.setColor(DyeColor.BLUE.getTextureDiffuseColor());
        final var saved = middle.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var loaded = new CableBlockEntity(middle.getBlockPos(), middle.getBlockState());
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(((Colored) loaded).getColor() == middleColor.getColor(), "Cable color was lost in NBT");
        helper.assertTrue(address.equals(loaded.node().address()), "Cable address was lost in NBT");
        helper.succeed();
    }

    private static CableBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CABLE.get().defaultBlockState());
        final var cable = (CableBlockEntity) helper.getLevel().getBlockEntity(pos);
        Network.joinOrCreateNetwork(helper.getLevel(), pos);
        return cable;
    }
}
