package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalClipboardPayload(int containerId, String value) implements CustomPacketPayload {
    public static final Type<TerminalClipboardPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_clipboard"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalClipboardPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalClipboardPayload::containerId,
        ByteBufCodecs.stringUtf8(32767),
        TerminalClipboardPayload::value,
        TerminalClipboardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
