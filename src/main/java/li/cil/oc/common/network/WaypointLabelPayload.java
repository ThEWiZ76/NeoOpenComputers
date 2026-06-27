package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WaypointLabelPayload(int containerId, String label) implements CustomPacketPayload {
    public static final Type<WaypointLabelPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "waypoint_label"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointLabelPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        WaypointLabelPayload::containerId,
        ByteBufCodecs.stringUtf8(32767),
        WaypointLabelPayload::label,
        WaypointLabelPayload::new);

    public WaypointLabelPayload {
        label = WaypointBlockEntity.truncateLabel(label);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
