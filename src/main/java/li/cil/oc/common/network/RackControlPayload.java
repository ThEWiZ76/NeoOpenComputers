package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RackControlPayload(int containerId, int slot, int action) implements CustomPacketPayload {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int TOGGLE = 2;

    public static final Type<RackControlPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "rack_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RackControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        RackControlPayload::containerId,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::slot,
        ByteBufCodecs.VAR_INT,
        RackControlPayload::action,
        RackControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
