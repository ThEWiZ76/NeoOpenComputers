package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.component.TerminalScreenDelta;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalScreenDeltaPayload(int containerId, TerminalScreenDelta delta) implements CustomPacketPayload {
    public static final Type<TerminalScreenDeltaPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_screen_delta"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScreenDeltaPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalScreenDeltaPayload::containerId,
        deltaCodec(),
        TerminalScreenDeltaPayload::delta,
        TerminalScreenDeltaPayload::new);

    public TerminalScreenDeltaPayload {
        delta = delta == null ? new TerminalScreenDelta(0, 0, new TerminalScreenDelta.Row[0]) : delta;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static StreamCodec<RegistryFriendlyByteBuf, TerminalScreenDelta> deltaCodec() {
        final StreamCodec<RegistryFriendlyByteBuf, CompoundTag> tagCodec = ByteBufCodecs.COMPOUND_TAG.cast();
        return tagCodec.map(TerminalScreenDelta::load, TerminalScreenDeltaPayload::saveDelta);
    }

    private static CompoundTag saveDelta(final TerminalScreenDelta delta) {
        final CompoundTag tag = new CompoundTag();
        delta.save(tag);
        return tag;
    }
}
