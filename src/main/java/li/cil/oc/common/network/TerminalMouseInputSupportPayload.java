package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalMouseInputSupportPayload(int containerId, boolean supportsMouseInput) implements CustomPacketPayload {
    public static final Type<TerminalMouseInputSupportPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_mouse_input_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMouseInputSupportPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalMouseInputSupportPayload::containerId,
        ByteBufCodecs.BOOL,
        TerminalMouseInputSupportPayload::supportsMouseInput,
        TerminalMouseInputSupportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
