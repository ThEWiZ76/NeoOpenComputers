package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RackOpenServerPayload(int containerId, int slot) implements CustomPacketPayload {
    public static final Type<RackOpenServerPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "rack_open_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RackOpenServerPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        RackOpenServerPayload::containerId,
        ByteBufCodecs.VAR_INT,
        RackOpenServerPayload::slot,
        RackOpenServerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
