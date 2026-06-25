package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalKeyPayload(int containerId, boolean pressed, int character, int keyCode) implements CustomPacketPayload {
    public static final Type<TerminalKeyPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalKeyPayload> STREAM_CODEC = StreamCodec.of(
        TerminalKeyPayload::encode,
        TerminalKeyPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buffer, final TerminalKeyPayload payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeBoolean(payload.pressed());
        buffer.writeChar(payload.character());
        buffer.writeInt(payload.keyCode());
    }

    private static TerminalKeyPayload decode(final RegistryFriendlyByteBuf buffer) {
        return new TerminalKeyPayload(
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readChar(),
            buffer.readInt());
    }
}
