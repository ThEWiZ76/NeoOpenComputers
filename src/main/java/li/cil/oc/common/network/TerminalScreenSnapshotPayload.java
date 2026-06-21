package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerminalScreenSnapshotPayload(int containerId, TerminalScreenSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<TerminalScreenSnapshotPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "terminal_screen_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScreenSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TerminalScreenSnapshotPayload::containerId,
        snapshotCodec(),
        TerminalScreenSnapshotPayload::snapshot,
        TerminalScreenSnapshotPayload::new);

    public TerminalScreenSnapshotPayload {
        snapshot = snapshot == null ? new TerminalScreenSnapshot(0, 0, new String[0]) : snapshot;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static StreamCodec<RegistryFriendlyByteBuf, TerminalScreenSnapshot> snapshotCodec() {
        final StreamCodec<RegistryFriendlyByteBuf, CompoundTag> tagCodec = ByteBufCodecs.COMPOUND_TAG.cast();
        return tagCodec.map(TerminalScreenSnapshot::load, TerminalScreenSnapshotPayload::saveSnapshot);
    }

    private static CompoundTag saveSnapshot(final TerminalScreenSnapshot snapshot) {
        final CompoundTag tag = new CompoundTag();
        snapshot.save(tag);
        return tag;
    }
}
