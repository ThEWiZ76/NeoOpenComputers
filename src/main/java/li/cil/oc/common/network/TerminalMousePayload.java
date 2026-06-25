package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalMousePayload(int containerId, int kind, double x, double y, int buttonOrDelta) implements CustomPacketPayload {
    public static final int MOUSE_DOWN = 0;
    public static final int MOUSE_DRAG = 1;
    public static final int MOUSE_UP = 2;
    public static final int MOUSE_SCROLL = 3;

    public static final Type<TerminalMousePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_mouse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMousePayload> STREAM_CODEC = StreamCodec.of(
        TerminalMousePayload::encode,
        TerminalMousePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(final RegistryFriendlyByteBuf buffer, final TerminalMousePayload payload) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeVarInt(payload.kind());
        buffer.writeFloat((float) payload.x());
        buffer.writeFloat((float) payload.y());
        buffer.writeByte(payload.buttonOrDelta());
    }

    private static TerminalMousePayload decode(final RegistryFriendlyByteBuf buffer) {
        return new TerminalMousePayload(
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readFloat(),
            buffer.readFloat(),
            buffer.readByte());
    }
}
