package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record NanomachinePowerPayload(
    boolean installed,
    double buffer,
    double maxBuffer,
    int activeInputs,
    int totalInputs,
    List<String> activeParticleEffects) implements CustomPacketPayload {
    public static final Type<NanomachinePowerPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachine_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NanomachinePowerPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        NanomachinePowerPayload::installed,
        ByteBufCodecs.DOUBLE,
        NanomachinePowerPayload::buffer,
        ByteBufCodecs.DOUBLE,
        NanomachinePowerPayload::maxBuffer,
        ByteBufCodecs.INT,
        NanomachinePowerPayload::activeInputs,
        ByteBufCodecs.INT,
        NanomachinePowerPayload::totalInputs,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)),
        NanomachinePowerPayload::activeParticleEffects,
        NanomachinePowerPayload::new);

    public NanomachinePowerPayload {
        activeParticleEffects = activeParticleEffects == null ? List.of() : List.copyOf(activeParticleEffects);
    }

    public NanomachinePowerPayload(final boolean installed, final double buffer, final double maxBuffer, final int activeInputs, final int totalInputs) {
        this(installed, buffer, maxBuffer, activeInputs, totalInputs, List.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
