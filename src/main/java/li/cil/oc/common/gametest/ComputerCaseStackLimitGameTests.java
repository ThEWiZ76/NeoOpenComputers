package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ComputerCaseStackLimitGameTests {
    @GameTest(template = "ae2_export")
    public static void shiftClickInstallsOneComponentAndPreservesRemainder(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var blocks = new net.minecraft.world.level.block.Block[]{ModBlocks.COMPUTER_CASE_TIER1.get(), ModBlocks.COMPUTER_CASE_TIER2.get(), ModBlocks.COMPUTER_CASE_TIER3.get()};
            for (int tier = 0; tier < blocks.length; tier++) {
                final var pos = helper.absolutePos(new BlockPos(2 + tier, 2, 2));
                helper.getLevel().setBlockAndUpdate(pos, blocks[tier].defaultBlockState());
                final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
                final var menu = new ComputerCaseMenu(81, player.getInventory(), computer);
                player.getInventory().setItem(9, new ItemStack(ModItems.EEPROM.get(), 64));
                final int source = ComputerCaseMenu.computerSlotCountForTier(tier);
                menu.quickMoveStack(player, source);
                final int destination = source - 1;
                helper.assertTrue(computer.getItem(destination).getCount() == 1, "Shift click installed a component stack in tier " + tier);
                helper.assertTrue(player.getInventory().getItem(9).getCount() == 63, "Shift click lost the component remainder");
                menu.quickMoveStack(player, source);
                helper.assertTrue(computer.getItem(destination).getCount() == 1 && player.getInventory().getItem(9).getCount() == 63, "Second shift click merged into occupied hardware");
            }
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void ordinaryAndClientPredictedSlotsLimitComponents(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            // The real client uses a SimpleContainer, whose default limit is 64.
            final var menu = new ComputerCaseMenu(82, player.getInventory());
            menu.setCarried(new ItemStack(ModItems.EEPROM.get(), 64));
            menu.clicked(6, 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getSlot(6).getItem().getCount() == 1 && menu.getCarried().getCount() == 63, "Predicted ordinary click accepts a hardware stack");
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.FLOPPY.get())), "Client prediction accepts a floppy in a card slot");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void legacyOverstackCanBeRecoveredWithoutItemLoss(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        final var saved = computer.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var items = new ListTag();
        final var legacy = (CompoundTag) new ItemStack(ModItems.EEPROM.get(), 62).save(helper.getLevel().registryAccess());
        legacy.putByte("Slot", (byte) 6); items.add(legacy); saved.put("Items", items);
        computer.loadWithComponents(saved, helper.getLevel().registryAccess());
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var menu = new ComputerCaseMenu(83, player.getInventory(), computer);
            menu.quickMoveStack(player, 6);
            int count = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                final var item = player.getInventory().getItem(slot);
                if (item.is(ModItems.EEPROM.get())) count += item.getCount();
            }
            helper.assertTrue(computer.getItem(6).isEmpty() && count == 62, "Recovering old illegal stack discarded or duplicated components");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
}
