package li.cil.oc.common.driver;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.HologramBlockEntity;
import li.cil.oc.common.blockentity.NetSplitterBlockEntity;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public final class BlockItemEnvironmentProvider implements EnvironmentProvider {
    private final Map<Block, Class<?>> environments = Map.ofEntries(
        Map.entry(ModBlocks.ASSEMBLER.get(), AssemblerBlockEntity.class),
        Map.entry(ModBlocks.COMPUTER_CASE_TIER1.get(), ComputerCaseBlockEntity.class),
        Map.entry(ModBlocks.COMPUTER_CASE_TIER2.get(), ComputerCaseBlockEntity.class),
        Map.entry(ModBlocks.COMPUTER_CASE_TIER3.get(), ComputerCaseBlockEntity.class),
        Map.entry(ModBlocks.HOLOGRAM_TIER1.get(), HologramBlockEntity.class),
        Map.entry(ModBlocks.HOLOGRAM_TIER2.get(), HologramBlockEntity.class),
        Map.entry(ModBlocks.PRINTER.get(), PrinterBlockEntity.class),
        Map.entry(ModBlocks.NET_SPLITTER.get(), NetSplitterBlockEntity.class),
        Map.entry(ModBlocks.REDSTONE_IO.get(), RedstoneIoBlockEntity.class),
        Map.entry(ModBlocks.RELAY.get(), RelayBlockEntity.class),
        Map.entry(ModBlocks.SCREEN_TIER1.get(), ScreenBlockEntity.class),
        Map.entry(ModBlocks.SCREEN_TIER2.get(), ScreenBlockEntity.class),
        Map.entry(ModBlocks.SCREEN_TIER3.get(), ScreenBlockEntity.class),
        Map.entry(ModBlocks.WAYPOINT.get(), WaypointBlockEntity.class)
    );

    @Override
    public Class<?> getEnvironment(final ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return environments.get(blockItem.getBlock());
    }
}
