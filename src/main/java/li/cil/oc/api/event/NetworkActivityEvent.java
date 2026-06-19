package li.cil.oc.api.event;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;

public class NetworkActivityEvent extends Event {
    protected Level world;
    protected double x;
    protected double y;
    protected double z;
    protected BlockEntity blockEntity;
    protected CompoundTag data;

    protected NetworkActivityEvent(final BlockEntity blockEntity, final CompoundTag data) {
        this.world = blockEntity == null ? null : blockEntity.getLevel();
        this.x = blockEntity == null ? 0 : blockEntity.getBlockPos().getX() + 0.5;
        this.y = blockEntity == null ? 0 : blockEntity.getBlockPos().getY() + 0.5;
        this.z = blockEntity == null ? 0 : blockEntity.getBlockPos().getZ() + 0.5;
        this.blockEntity = blockEntity;
        this.data = data;
    }

    protected NetworkActivityEvent(final Level world, final double x, final double y, final double z, final CompoundTag data) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
    }

    public Level getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public BlockEntity getTileEntity() {
        return blockEntity;
    }

    public CompoundTag getData() {
        return data;
    }

    public static final class Server extends NetworkActivityEvent {
        private final Node node;

        public Server(final BlockEntity blockEntity, final Node node) {
            super(blockEntity, new CompoundTag());
            this.node = node;
        }

        public Server(final Level world, final double x, final double y, final double z, final Node node) {
            super(world, x, y, z, new CompoundTag());
            this.node = node;
        }

        public Node getNode() {
            return node;
        }
    }

    public static final class Client extends NetworkActivityEvent {
        public Client(final BlockEntity blockEntity, final CompoundTag data) {
            super(blockEntity, data);
        }

        public Client(final Level world, final double x, final double y, final double z, final CompoundTag data) {
            super(world, x, y, z, data);
        }
    }
}
