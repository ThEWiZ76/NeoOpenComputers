package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class NoteBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockState(pos).is(Blocks.NOTE_BLOCK);
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (worksWith(world, pos, side)) {
            return new Environment(world, pos);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Note Block",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Note Block"
        );

        private final Level level;
        private final BlockPos pos;

        public Environment(final Level level, final BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("note_block", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "note_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
        public Object[] getPitch(final Context context, final Arguments arguments) {
            return new Object[]{state().getValue(NoteBlock.NOTE) + 1};
        }

        @Callback(doc = "function(value:number):boolean -- Set the pitch for this note block. Must be in the interval [1, 25].")
        public Object[] setPitch(final Context context, final Arguments arguments) {
            setPitch(arguments.checkInteger(0));
            return new Object[]{true};
        }

        @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch first to save a tick.")
        public Object[] trigger(final Context context, final Arguments arguments) {
            if (arguments.count() > 0 && arguments.checkAny(0) != null) {
                setPitch(arguments.checkInteger(0));
            }
            final boolean canTrigger = level.getBlockState(pos.above()).isAir();
            if (canTrigger) {
                level.blockEvent(pos, Blocks.NOTE_BLOCK, 0, 0);
            }
            return new Object[]{canTrigger};
        }

        private void setPitch(final int value) {
            if (value < 1 || value > 25) {
                throw new IllegalArgumentException("invalid pitch");
            }
            level.setBlock(pos, state().setValue(NoteBlock.NOTE, value - 1), 3);
        }

        private BlockState state() {
            final BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.NOTE_BLOCK)) {
                throw new IllegalStateException("note block is no longer present");
            }
            return state;
        }
    }
}
