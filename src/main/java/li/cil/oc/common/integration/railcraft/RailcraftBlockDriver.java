package li.cil.oc.common.integration.railcraft;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import mods.railcraft.world.level.block.entity.worldspike.PersonalWorldSpikeBlockEntity;
import mods.railcraft.world.level.block.entity.worldspike.WorldSpikeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class RailcraftBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        return world != null && pos != null && world.hasChunkAt(pos) && world.getBlockEntity(pos) instanceof WorldSpikeBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return worksWith(world, pos, side) ? new Environment((WorldSpikeBlockEntity) world.getBlockEntity(pos)) : null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
        private final WorldSpikeBlockEntity tile;

        Environment(WorldSpikeBlockEntity tile) {
            this.tile = tile;
            setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent("worldspike", Visibility.Network).create());
        }

        @Override public String preferredName() { return "worldspike"; }
        @Override public int priority() { return 5; }

        private WorldSpikeBlockEntity tile() {
            final var world = tile.getLevel();
            if (tile.isRemoved() || world == null || !world.hasChunkAt(tile.getBlockPos()) || world.getBlockEntity(tile.getBlockPos()) != tile) {
                throw new IllegalStateException("Railcraft worldspike is no longer present");
            }
            return tile;
        }

        @Callback(doc = "function():int -- Get the fuel amount (zero for fuel-free worldspikes).")
        public Object[] getFuel(Context context, Arguments args) {
            tile();
            // Railcraft Reborn 1.2 worldspikes have no fuel inventory or burn counter.
            return new Object[]{0};
        }

        @Callback(doc = "function():string -- Get the worldspike owner name, if known.")
        public Object[] getOwner(Context context, Arguments args) {
            final var owner = tile().getOwner().map(com.mojang.authlib.GameProfile::getName).orElse(null);
            return owner == null || owner.equals("[unknown]") ? null : new Object[]{owner};
        }

        @Callback(doc = "function():string -- Get the worldspike type.")
        public Object[] getType(Context context, Arguments args) {
            return new Object[]{tile() instanceof PersonalWorldSpikeBlockEntity ? "personal" : "world"};
        }

        @Callback(doc = "function():table -- Get the fuel-slot contents, if this worldspike needs fuel.")
        public Object[] getFuelSlotContents(Context context, Arguments args) { tile(); return null; }

        @Callback(doc = "function():boolean -- Get whether redstone disables this worldspike.")
        public Object[] isDisabled(Context context, Arguments args) {
            tile();
            // These provider blocks have no redstone disable state.
            return new Object[]{false};
        }
    }
}
