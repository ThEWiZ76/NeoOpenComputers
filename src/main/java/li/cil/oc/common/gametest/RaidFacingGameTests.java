package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.menu.RaidMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RaidFacingGameTests {
    private static final Direction[] FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    @GameTest(template = "ae2_export")
    public static void raidItemFacesPlacingPlayerInAllDirections(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel(); player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        try {
            for (int index = 0; index < FACES.length; index++) {
                final var view = FACES[index]; final var pos = helper.absolutePos(new BlockPos(2 + index, 2, 2));
                helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                final var standing = pos.relative(view.getOpposite(), 2); player.setPos(standing.getX() + 0.5, standing.getY(), standing.getZ() + 0.5); player.setYRot(view.toYRot()); player.setXRot(0);
                final var stack = new ItemStack(ModItems.RAID.get()); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                final var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, 0.5, 0), Direction.UP, pos.below(), false);
                helper.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Actual RAID item placement failed");
                final var state = helper.getLevel().getBlockState(pos);
                helper.assertTrue(state.is(ModBlocks.RAID.get()) && state.hasProperty(HorizontalDirectionalBlock.FACING)
                    && state.getValue(HorizontalDirectionalBlock.FACING) == view.getOpposite(), "Placed RAID front does not face the placing player");
            }
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidStateRotationAndMirrorsKeepHorizontalFront(GameTestHelper helper) {
        final var initial = ModBlocks.RAID.get().defaultBlockState();
        helper.assertTrue(initial.hasProperty(HorizontalDirectionalBlock.FACING), "RAID lacks horizontal facing state");
        for (var face : FACES) {
            final var state = initial.setValue(HorizontalDirectionalBlock.FACING, face);
            for (var rotation : Rotation.values()) helper.assertTrue(state.rotate(rotation).getValue(HorizontalDirectionalBlock.FACING) == rotation.rotate(face), "RAID structure rotation changed the wrong front");
            for (var mirror : Mirror.values()) helper.assertTrue(state.mirror(mirror).getValue(HorizontalDirectionalBlock.FACING) == mirror.mirror(face), "RAID structure mirror changed the wrong front");
        }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void realWrenchRotationPreservesRaidAndOpenFiles(GameTestHelper helper) throws Exception {
        final var hardware = RaidStorageGameTests.hardware(helper); RaidStorageGameTests.fill(hardware);
        final var handle = RaidStorageGameTests.write(helper, hardware); final var fs = RaidStorageGameTests.filesystem(helper, hardware.raid());
        final var player = helper.makeMockServerPlayerInLevel(); final var pos = hardware.raid().getBlockPos();
        try {
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
            final var initial = hardware.raid().getBlockState(); helper.assertTrue(initial.hasProperty(HorizontalDirectionalBlock.FACING), "RAID cannot be rotated by a wrench");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
            final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            for (int step = 0; step < 4; step++) {
                final var before = helper.getLevel().getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
                helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction(), "Player wrench interaction did not rotate RAID");
                helper.assertTrue(player.containerMenu == player.inventoryMenu, "Wrench rotation opened the RAID menu");
                helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING) == before.getClockWise()
                    && helper.getLevel().getBlockEntity(pos) == hardware.raid() && RaidStorageGameTests.filesystem(helper, hardware.raid()) == fs
                    && hardware.raid().diskCount() == 3, "Wrench rotation replaced the RAID host, disks or live filesystem");
            }
            helper.assertTrue(helper.getLevel().getBlockState(pos) == initial, "Four wrench turns did not return to initial state");
            helper.assertTrue(java.util.Arrays.equals((byte[]) fs.invoke("read", hardware.computer().machine(), handle, 3)[0], new byte[]{(byte) 128, (byte) 255, 42}), "Wrench rotation closed or reset an open RAID file");
            fs.invoke("close", hardware.computer().machine(), handle); hardware.raid().clearContent();
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void wrenchAllowsNonRotatableBlockInteraction(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        try {
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
            final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
            helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction(), "Wrench swallowed nonrotatable block interaction");
            helper.assertTrue(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu, "Wrench prevented crafting table menu");
        } finally { player.closeContainer(); helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
    @GameTest(template = "ae2_export")
    public static void raidMenuRequiresCurrentHostWithinEightBlocks(GameTestHelper helper) {
        final var hardware = RaidStorageGameTests.hardware(helper); final var raid = hardware.raid(); final var pos = raid.getBlockPos();
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var menu = new RaidMenu(1, player.getInventory(), raid);
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5); helper.assertTrue(menu.stillValid(player), "Nearby RAID menu is not valid");
            player.setPos(pos.getX() + 8.5, pos.getY() + 0.5, pos.getZ() + 0.5); helper.assertTrue(menu.stillValid(player), "RAID menu rejects exact eight-block boundary");
            player.setPos(pos.getX() + 8.625, pos.getY() + 0.5, pos.getZ() + 0.5); helper.assertTrue(!menu.stillValid(player), "RAID menu remains usable beyond eight blocks");
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.getLevel().removeBlockEntity(pos); final var replacement = new RaidBlockEntity(pos, raid.getBlockState()); replacement.setLevel(helper.getLevel()); helper.getLevel().setBlockEntity(replacement);
            helper.assertTrue(!menu.stillValid(player), "RAID menu accepts a replaced host");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
}
