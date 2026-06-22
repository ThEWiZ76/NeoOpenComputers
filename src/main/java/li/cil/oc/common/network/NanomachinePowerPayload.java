package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NanomachinePowerPayload(boolean installed, double buffer, double maxBuffer, int activeInputs, int totalInputs) implements CustomPacketPayload {
    public static final Type<NanomachinePowerPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachine_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NanomachinePowerPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        NanomachinePowerPayload::installed,
        ByteBufCodecs.DOUBLE,
        NanomachinePowerPayload::buffer,
        ByteBufCodecs.DOUBLE,
        NanomachinePowerPayload::maxBuffer,
        ByteBufCodecs.INT,
        NanomachinePowerPayload::activeInputs,
        ByteBufCodecs.INT,
        NanomachinePowerPayload::totalInputs,
        NanomachinePowerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
