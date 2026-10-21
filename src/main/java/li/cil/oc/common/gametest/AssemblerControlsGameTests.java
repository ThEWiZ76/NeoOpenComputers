package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.menu.AssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AssemblerControlsGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void survivalSneakAssemblyRequiresEnergyInsteadOfInstantCompletion(GameTestHelper helper) {
        final var host = fixture(helper); final var player = player(helper, host, GameType.SURVIVAL);
        try {
            player.setShiftKeyDown(true);
            final var result = host.getBlockState().useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(host.getBlockPos()), Direction.NORTH, host.getBlockPos(), false));
            helper.assertTrue(result.consumesAction() && host.isAssembling() && host.getItem(0).isEmpty(),
                "Survival sneak interaction completed assembly without energy");
            helper.assertTrue(host.saveWithFullMetadata(helper.getLevel().registryAccess()).getDouble("remainingEnergy") > 0D,
                "Survival shortcut has no paid work remaining");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalAssemblerButtonQueuesOnlyOnePaidJob(GameTestHelper helper) {
        final var host = fixture(helper); final var player = player(helper, host, GameType.SURVIVAL);
        try {
            final var menu = new AssemblerMenu(1, player.getInventory(), host); player.containerMenu = menu;
            helper.assertTrue(menu.clickMenuButton(player, 0), "Valid Survival GUI start was rejected");
            final var prepared = host.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(host.isAssembling() && host.getItem(0).isEmpty() && menu.assemblyState() == AssemblerMenu.STATE_BUSY
                && prepared.getDouble("remainingEnergy") > 0D, "Survival GUI start bypassed the paid pending job");
            helper.assertTrue(!menu.clickMenuButton(player, 0), "Repeated GUI start accepted a busy assembler");
            final var repeated = host.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(prepared.getDouble("remainingEnergy") == repeated.getDouble("remainingEnergy")
                && prepared.getCompound("output").equals(repeated.getCompound("output")), "Repeated GUI start replaced owned output/payment");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeAssemblerButtonCompletesImmediatelyWithoutClientPrivilegeFlags(GameTestHelper helper) {
        final var host = fixture(helper); final var player = player(helper, host, GameType.CREATIVE);
        try {
            final var menu = new AssemblerMenu(1, player.getInventory(), host); player.containerMenu = menu;
            helper.assertTrue(menu.clickMenuButton(player, 0), "Valid creative GUI start was rejected");
            helper.assertTrue(!host.isAssembling() && host.getItem(0).is(ModItems.TABLET.get()) && host.getItem(0).getCount() == 1,
                "Creative GUI start did not return exactly one completed tablet");
            for (int slot = 1; slot < host.getContainerSize(); slot++) helper.assertTrue(host.getItem(slot).isEmpty(), "Creative GUI assembly left duplicate ingredients");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerButtonRejectsUnknownStaleDistantAndOtherPlayerRequests(GameTestHelper helper) {
        final var host = fixture(helper); final var player = player(helper, host, GameType.SURVIVAL);
        final var other = player(helper, host, GameType.CREATIVE);
        try {
            final var menu = new AssemblerMenu(1, player.getInventory(), host); player.containerMenu = menu;
            helper.assertTrue(!menu.clickMenuButton(player, 127) && !menu.clickMenuButton(null, 0)
                && !menu.clickMenuButton(other, 0), "Assembler accepted an unknown button or unbound player");
            player.setPos(host.getBlockPos().getX() + 16, host.getBlockPos().getY(), host.getBlockPos().getZ());
            helper.assertTrue(!menu.clickMenuButton(player, 0), "Distant player started an assembler");
            player.setPos(Vec3.atCenterOf(host.getBlockPos())); player.containerMenu = player.inventoryMenu;
            helper.assertTrue(!menu.clickMenuButton(player, 0), "Closed/stale menu started an assembler");
            helper.assertTrue(host.canAssemble() && !host.isAssembling() && host.getItem(0).is(ModItems.TABLET_CASE_TIER1.get()),
                "Rejected start request consumed its input");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.getLevel().getServer().getPlayerList().remove(other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerLiveNbtReloadRemovesSlotsAbsentFromReplacementData(GameTestHelper helper) {
        final var host = fixture(helper);
        final var replacement = host.saveWithFullMetadata(helper.getLevel().registryAccess()); replacement.put("Items", new ListTag());
        host.loadWithComponents(replacement, helper.getLevel().registryAccess());
        for (int slot = 0; slot < host.getContainerSize(); slot++) helper.assertTrue(host.getItem(slot).isEmpty(),
            "Replacing assembler inventory NBT retained an absent old slot");
        helper.succeed();
    }

    private static AssemblerBlockEntity fixture(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity host = helper.getBlockEntity(POS);
        host.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(ModItems.TABLET_CASE_TIER1.get()));
        host.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, new ItemStack(ModItems.CPU_TIER1.get()));
        host.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        helper.assertTrue(host.canAssemble(), "Assembler control fixture is invalid");
        return host;
    }

    private static ServerPlayer player(GameTestHelper helper, AssemblerBlockEntity host, GameType mode) {
        final var player = helper.makeMockServerPlayerInLevel(); player.setGameMode(mode); player.setPos(Vec3.atCenterOf(host.getBlockPos()));
        return player;
    }
}
