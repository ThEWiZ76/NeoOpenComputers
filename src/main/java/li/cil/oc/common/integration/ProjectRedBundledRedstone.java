package li.cil.oc.common.integration;

import li.cil.oc.common.component.RedstoneControllerHost;
import mrtjp.projectred.api.IBundledTileInteraction;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Optional ProjectRed transmission bridge; no optional interfaces on core block entities. */
public final class ProjectRedBundledRedstone {
    private static boolean available;

    public static void register() {
        if (!available && ModList.get().isLoaded("projectred_transmission")) {
            available = Loaded.register();
        }
    }

    public static boolean isAvailable() { return available; }

    public static byte[] input(final RedstoneControllerHost host, final Direction side) {
        final var world = host.world();
        if (!available || world == null) return null;
        final var pos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        if (!world.hasChunkAt(pos.relative(side))) return null;
        // Unlike CC's emitter-face query, ProjectRed takes the receiver position and side.
        return Loaded.input(world, pos, side);
    }

    // Keep optional argument/interface types out of the always-loaded class's verifier paths.
    private static final class Loaded {
        static boolean register() {
            if (ProjectRedAPI.transmissionAPI == null) return false;
            ProjectRedAPI.transmissionAPI.registerBundledTileInteraction(new Interaction());
            return true;
        }

        static byte[] input(Level world, BlockPos pos, Direction side) {
            return ProjectRedAPI.transmissionAPI.getBundledInput(world, pos, side);
        }
    }

    private static final class Interaction implements IBundledTileInteraction {
        @Override
        public boolean isValidInteractionFor(Level world, BlockPos pos, Direction side) {
            return world.hasChunkAt(pos) && world.getBlockEntity(pos) instanceof RedstoneControllerHost;
        }

        @Override
        public boolean canConnectBundled(Level world, BlockPos pos, Direction side) {
            return isValidInteractionFor(world, pos, side);
        }

        @Override
        public byte[] getBundledSignal(Level world, BlockPos pos, Direction side) {
            if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof RedstoneControllerHost host)) return null;
            final var signal = new byte[16];
            for (int color = 0; color < 16; color++) signal[color] = (byte) Math.clamp(host.bundledRedstoneOutput(side, color), 0, 255);
            return signal;
        }
    }

    private ProjectRedBundledRedstone() {}
}
