package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalKeyPayload(int containerId, boolean pressed, int character, int keyCode) implements CustomPacketPayload {
    public static final Type<TerminalKeyPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalKeyPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalKeyPayload::containerId,
        ByteBufCodecs.BOOL,
        TerminalKeyPayload::pressed,
        ByteBufCodecs.VAR_INT,
        TerminalKeyPayload::character,
        ByteBufCodecs.VAR_INT,
        TerminalKeyPayload::keyCode,
        TerminalKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
