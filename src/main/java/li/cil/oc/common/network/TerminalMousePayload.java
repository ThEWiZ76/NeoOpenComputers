package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMousePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalMousePayload::containerId,
        ByteBufCodecs.VAR_INT,
        TerminalMousePayload::kind,
        ByteBufCodecs.DOUBLE,
        TerminalMousePayload::x,
        ByteBufCodecs.DOUBLE,
        TerminalMousePayload::y,
        ByteBufCodecs.VAR_INT,
        TerminalMousePayload::buttonOrDelta,
        TerminalMousePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
