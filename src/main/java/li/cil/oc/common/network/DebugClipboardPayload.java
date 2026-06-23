package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DebugClipboardPayload(String value) implements CustomPacketPayload {
    public static final Type<DebugClipboardPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "debug_clipboard"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DebugClipboardPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(32767),
        DebugClipboardPayload::value,
        DebugClipboardPayload::new);

    public DebugClipboardPayload {
        value = value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
