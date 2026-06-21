package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerRackControlPayload(int containerId, int action) implements CustomPacketPayload {
    public static final Type<ServerRackControlPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "server_rack_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerRackControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ServerRackControlPayload::containerId,
        ByteBufCodecs.VAR_INT,
        ServerRackControlPayload::action,
        ServerRackControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
