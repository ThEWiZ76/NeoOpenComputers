package li.cil.oc.common.integration;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.DriverRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps optional EnderStorage classes behind the installed-mod check. */
public final class EnderStorageIntegration {
    private EnderStorageIntegration() { }

    public static void register(final DriverRegistry registry) {
        if (ModList.get() != null && ModList.get().isLoaded("enderstorage")) Loaded.register(registry);
    }

    private static final class Loaded {
        static void register(final DriverRegistry registry) { registry.add(new StorageDriver()); }

        private static final class StorageDriver implements DriverBlock {
            @Override
            public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
                return world != null && pos != null && world.hasChunkAt(pos)
                    && world.getBlockEntity(pos) instanceof codechicken.enderstorage.tile.TileFrequencyOwner;
            }

            @Override
            public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                return worksWith(world, pos, side)
                    ? new Environment((codechicken.enderstorage.tile.TileFrequencyOwner) world.getBlockEntity(pos)) : null;
            }
        }

        public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
            private final codechicken.enderstorage.tile.TileFrequencyOwner tile;

            Environment(final codechicken.enderstorage.tile.TileFrequencyOwner tile) {
                this.tile = tile;
                setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network)
                    .withComponent(preferredName(), Visibility.Network).create());
            }

            @Override
            public String preferredName() {
                return tile instanceof codechicken.enderstorage.tile.TileEnderTank ? "ender_tank" : "ender_chest";
            }

            @Override
            public int priority() { return 0; }

            @Callback(doc = "function():table -- Get the currently set frequency. {left, middle, right}")
            public Object[] getFrequency(final Context context, final Arguments args) {
                final var frequency = frequency();
                return new Object[]{new Integer[]{frequency.left().ordinal(), frequency.middle().ordinal(), frequency.right().ordinal()}};
            }

            @Callback(doc = "function(left:number, middle:number, right:number) -- Set the frequency. Range 0-15; also accepts a packed 0-4095 value.")
            public Object[] setFrequency(final Context context, final Arguments args) {
                final int left;
                final int middle;
                final int right;
                if (args.count() == 1) {
                    final int packed = args.checkInteger(0);
                    if ((packed & 0xFFF) != packed) throw new IllegalArgumentException("invalid frequency");
                    left = packed >> 8;
                    middle = (packed >> 4) & 15;
                    right = packed & 15;
                } else {
                    left = args.checkInteger(0);
                    middle = args.checkInteger(1);
                    right = args.checkInteger(2);
                    if ((left & 15) != left || (middle & 15) != middle || (right & 15) != right) {
                        throw new IllegalArgumentException("invalid frequency");
                    }
                }
                final var previous = frequency();
                tile.setFreq(new codechicken.enderstorage.api.Frequency(
                    codechicken.lib.colour.EnumColour.fromWoolMeta(left), codechicken.lib.colour.EnumColour.fromWoolMeta(middle),
                    codechicken.lib.colour.EnumColour.fromWoolMeta(right), previous.owner(), previous.ownerName()));
                return null;
            }

            @Callback(doc = "function():string -- Get the owner's name, UUID if its name is unavailable, or 'global' for public storage.")
            public Object[] getOwner(final Context context, final Arguments args) {
                final var frequency = frequency();
                return new Object[]{frequency.owner().isEmpty() ? "global"
                    : frequency.ownerName().map(net.minecraft.network.chat.Component::getString).orElseGet(() -> frequency.owner().orElseThrow().toString())};
            }

            @Callback(doc = "function():table -- Get the current frequency as color names.")
            public Object[] getFrequencyColors(final Context context, final Arguments args) {
                return new Object[]{frequency().toArray()};
            }

            @Callback(doc = "function():table -- Map zero-based frequency numbers to color names.")
            public Object[] getColors(final Context context, final Arguments args) {
                final Map<Integer, codechicken.lib.colour.EnumColour> colors = new LinkedHashMap<>();
                for (final var color : codechicken.lib.colour.EnumColour.values()) colors.put(color.ordinal(), color);
                return new Object[]{colors};
            }

            private codechicken.enderstorage.api.Frequency frequency() {
                final var world = tile.getLevel();
                if (tile.isRemoved() || world == null || !world.hasChunkAt(tile.getBlockPos()) || world.getBlockEntity(tile.getBlockPos()) != tile) {
                    throw new IllegalStateException("ender storage block is no longer present");
                }
                return tile.getFrequency();
            }
        }
    }
}
