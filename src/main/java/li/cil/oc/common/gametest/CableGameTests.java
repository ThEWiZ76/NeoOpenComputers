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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class CableGameTests {
    @GameTest(template = "empty")
    public static void cableConnectionsUseAllSixAxes(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var center = place(helper, pos);
        center.setColor(DyeColor.RED.getTextureDiffuseColor());
        for (final var side : Direction.values()) {
            final var neighborPos = pos.relative(side);
            final var neighbor = place(helper, neighborPos);
            assertConnection(helper, pos, side, "cable");
            assertConnection(helper, neighborPos, side.getOpposite(), "cable");
            final var expected = new net.minecraft.world.phys.AABB(
                side.getStepX() < 0 ? 0 : .375, side.getStepY() < 0 ? 0 : .375, side.getStepZ() < 0 ? 0 : .375,
                side.getStepX() > 0 ? 1 : .625, side.getStepY() > 0 ? 1 : .625, side.getStepZ() > 0 ? 1 : .625);
            helper.assertTrue(world.getBlockState(pos).getCollisionShape(world, pos).bounds().equals(expected),
                "Cable collision arm has incorrect direction on " + side);
            neighbor.setColor(DyeColor.BLUE.getTextureDiffuseColor());
            assertConnection(helper, pos, side, "none");
            assertConnection(helper, neighborPos, side.getOpposite(), "none");
            neighbor.setColor(DyeColor.RED.getTextureDiffuseColor());
            assertConnection(helper, pos, side, "cable");
            world.removeBlock(neighborPos, false);
            assertConnection(helper, pos, side, "none");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableShapeTracksCompatiblePortsAndRecoloring(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final var cable = place(helper, pos);
        final var isolated = world.getBlockState(pos).getShape(world, pos);
        helper.assertTrue(isolated.bounds().equals(new net.minecraft.world.phys.AABB(.375, .375, .375, .625, .625, .625)),
            "Isolated cable is not the upstream quarter-block center");
        helper.assertTrue(!world.getBlockState(pos).isCollisionShapeFullBlock(world, pos), "Cable still collides as a full cube");
        helper.assertTrue(isolated.clip(Vec3.atLowerCornerOf(pos).add(-.5, .1, .1),
            Vec3.atLowerCornerOf(pos).add(1.5, .1, .1), pos) == null, "Cable raytrace hits empty corner space");
        final var east = place(helper, pos.east());
        assertConnection(helper, pos, Direction.EAST, "cable");
        assertConnection(helper, pos.east(), Direction.WEST, "cable");
        final var connected = world.getBlockState(pos).getCollisionShape(world, pos, CollisionContext.empty());
        helper.assertTrue(connected.bounds().equals(new net.minecraft.world.phys.AABB(.375, .375, .375, 1, .625, .625)),
            "Cable connection did not extend collision shape to the eastern neighbor");
        world.setBlockAndUpdate(pos.above(), ModBlocks.CAPACITOR.get().defaultBlockState());
        assertConnection(helper, pos, Direction.UP, "device");
        world.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
        assertConnection(helper, pos, Direction.NORTH, "none");
        world.setBlockAndUpdate(pos.west(), ModBlocks.NET_SPLITTER.get().defaultBlockState());
        assertConnection(helper, pos, Direction.WEST, "none");
        world.setBlockAndUpdate(pos.west().above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        assertConnection(helper, pos, Direction.WEST, "device");
        world.removeBlock(pos.west().above(), false);
        assertConnection(helper, pos, Direction.WEST, "none");
        cable.setColor(DyeColor.RED.getTextureDiffuseColor());
        east.setColor(DyeColor.BLUE.getTextureDiffuseColor());
        assertConnection(helper, pos, Direction.EAST, "none");
        assertConnection(helper, pos.east(), Direction.WEST, "none");
        east.setColor(CableBlockEntity.DEFAULT_COLOR);
        assertConnection(helper, pos, Direction.EAST, "cable");
        assertConnection(helper, pos.east(), Direction.WEST, "cable");
        world.removeBlock(pos.east(), false);
        assertConnection(helper, pos, Direction.EAST, "none");
        world.removeBlock(pos.above(), false);
        helper.assertTrue(world.getBlockState(pos).getShape(world, pos).bounds().equals(isolated.bounds()),
            "Removing connections did not restore the isolated cable shape");
        helper.succeed();
    }

    private static void assertConnection(GameTestHelper helper, BlockPos pos, Direction side, String expected) {
        final BlockState state = helper.getLevel().getBlockState(pos);
        final var property = state.getProperties().stream().filter(value -> value.getName().equals(side.getName())).findFirst();
        helper.assertTrue(property.isPresent(), "Cable has no synchronized connection state for " + side);
        helper.assertTrue(state.getValue(property.orElseThrow()).toString().equalsIgnoreCase(expected),
            "Cable connection on " + side + " was not " + expected);
    }

    @GameTest(template = "empty")
    public static void cableColorRecipesMixAndWashWithoutLosingData(GameTestHelper helper) {
        final var recipes = helper.getLevel().getRecipeManager();
        final var colorHolder = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "colorize_cable"));
        helper.assertTrue(colorHolder.isPresent(), "Cable color mixing recipe is missing");
        final var washHolder = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "decolorize_cable"));
        helper.assertTrue(washHolder.isPresent(), "Cable washing recipe is missing");
        @SuppressWarnings("unchecked")
        final var color = (net.minecraft.world.item.crafting.Recipe<CraftingInput>) colorHolder.orElseThrow().value();
        @SuppressWarnings("unchecked")
        final var wash = (net.minecraft.world.item.crafting.Recipe<CraftingInput>) washHolder.orElseThrow().value();
        final var cable = new ItemStack(ModItems.CABLE.get(), 4);
        cable.set(DataComponents.CUSTOM_NAME, Component.literal("fixture-cable"));
        final var input = CraftingInput.of(3, 1, List.of(cable, new ItemStack(Items.RED_DYE, 2), new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(color.matches(input, helper.getLevel()), "Valid cable and multiple dyes did not match");
        final var mixed = color.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(mixed.is(ModItems.CABLE.get()) && mixed.getCount() == 1
            && mixed.get(DataComponents.DYED_COLOR).rgb() == 0xAD5398, "Red/blue blend did not preserve upstream brightness");
        helper.assertTrue(Component.literal("fixture-cable").equals(mixed.get(DataComponents.CUSTOM_NAME))
            && cable.getCount() == 4 && !cable.has(DataComponents.DYED_COLOR), "Crafting changed input or lost unrelated components");
        final var red = cable.copy();
        red.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.RED.getTextureDiffuseColor(), true));
        final var recolorInput = CraftingInput.of(2, 1, List.of(red, new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(color.assemble(recolorInput, helper.getLevel().registryAccess()).get(DataComponents.DYED_COLOR).rgb() == 0xAD5398,
            "Existing cable color was not included in dye blending");
        for (final var invalid : List.of(
            CraftingInput.of(1, 1, List.of(cable)),
            CraftingInput.of(3, 1, List.of(cable, cable.copy(), new ItemStack(Items.RED_DYE))),
            CraftingInput.of(2, 1, List.of(cable, new ItemStack(Items.DIAMOND))),
            CraftingInput.of(2, 1, List.of(new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.RED_DYE))))) {
            helper.assertTrue(!color.matches(invalid, helper.getLevel()), "Cable color recipe accepted invalid or non-cable input");
        }
        final var washInput = CraftingInput.of(2, 1, List.of(mixed, new ItemStack(Items.WATER_BUCKET)));
        helper.assertTrue(wash.matches(washInput, helper.getLevel()), "Cable and water bucket did not match wash recipe");
        final var cleaned = wash.assemble(washInput, helper.getLevel().registryAccess());
        helper.assertTrue(cleaned.getCount() == 1 && !cleaned.has(DataComponents.DYED_COLOR)
            && Component.literal("fixture-cable").equals(cleaned.get(DataComponents.CUSTOM_NAME))
            && mixed.has(DataComponents.DYED_COLOR), "Washing lost unrelated data or mutated its input");
        final var remainder = wash.getRemainingItems(washInput);
        helper.assertTrue(remainder.get(0).isEmpty() && remainder.get(1).is(Items.BUCKET) && remainder.get(1).getCount() == 1,
            "Washing did not return exactly one empty bucket");
        helper.assertTrue(!wash.matches(CraftingInput.of(2, 1, List.of(mixed, new ItemStack(Items.LAVA_BUCKET))), helper.getLevel())
            && !wash.matches(input, helper.getLevel()), "Washing accepted a non-water ingredient");
        helper.succeed();
    }

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
