package li.cil.oc.common.integration;

import dan200.computercraft.api.ComputerCraftAPI;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Optional CC integration. Only register after common setup has initialized both mods. */
public final class ComputerCraftBundledRedstone {
    private static boolean available;

    public static void register() {
        if (!available && ModList.get().isLoaded("computercraft")) {
            ComputerCraftAPI.registerBundledRedstoneProvider(ComputerCraftBundledRedstone::output);
            available = true;
        }
    }

    public static boolean isAvailable() { return available; }

    public static int input(final RedstoneControllerHost host, final Direction side) {
        final Level world = host.world();
        if (!available || world == null) return 0;
        final BlockPos target = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition()).relative(side);
        if (!world.hasChunkAt(target)) return 0;
        return Math.max(0, ComputerCraftAPI.getBundledRedstoneOutput(world, target, side.getOpposite()));
    }

    private static int output(final Level world, final BlockPos pos, final Direction side) {
        if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof RedstoneControllerHost host)) return -1;
        int result = 0;
        for (int color = 0; color < 16; color++) {
            if (host.bundledRedstoneOutput(side, color) > 0) result |= 1 << color;
        }
        return result;
    }

    private ComputerCraftBundledRedstone() {}
}
