package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ComputerCaseControlPayload(int containerId, int action) implements CustomPacketPayload {
    public static final Type<ComputerCaseControlPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "computer_case_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerCaseControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ComputerCaseControlPayload::containerId,
        ByteBufCodecs.VAR_INT,
        ComputerCaseControlPayload::action,
        ComputerCaseControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
