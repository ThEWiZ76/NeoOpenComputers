package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MicrocontrollerControlPayload(int containerId, int action) implements CustomPacketPayload {
    public static final Type<MicrocontrollerControlPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "microcontroller_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MicrocontrollerControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MicrocontrollerControlPayload::containerId,
        ByteBufCodecs.VAR_INT,
        MicrocontrollerControlPayload::action,
        MicrocontrollerControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
