package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RackControlPayload(int containerId, int slot, int action, int connectableIndex, int side) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int TOGGLE = 2;
    public static final int MAP = 3;
    public static final int RELAY = 4;
    public static final int PRIMARY_CONNECTABLE = -1;
    public static final int NO_SIDE = -1;

    public static final Type<RackControlPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "rack_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RackControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        RackControlPayload::containerId,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::slot,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::action,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::connectableIndex,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::side,
        RackControlPayload::new);

    public RackControlPayload(final int containerId, final int slot, final int action) {
        this(containerId, slot, action, PRIMARY_CONNECTABLE, NO_SIDE);
    }

    public static RackControlPayload map(final int containerId, final int slot, final int connectableIndex, final Direction side) {
        return new RackControlPayload(containerId, slot, MAP, connectableIndex, side == null ? NO_SIDE : side.ordinal());
    }

    public static RackControlPayload relay(final int containerId, final boolean enabled) {
        return new RackControlPayload(containerId, 0, RELAY, PRIMARY_CONNECTABLE, enabled ? 1 : 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
