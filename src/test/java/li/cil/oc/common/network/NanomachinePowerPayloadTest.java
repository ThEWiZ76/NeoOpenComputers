package li.cil.oc.common.network;

import io.netty.buffer.Unpooled;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NanomachinePowerPayloadTest {
    @Test
    void exposesStablePayloadType() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachine_power"),
            NanomachinePowerPayload.TYPE.id());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        final NanomachinePowerPayload payload = new NanomachinePowerPayload(true, 12.5D, 100D, 3, 9, List.of("flame", "heart"));
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.NEOFORGE);

        NanomachinePowerPayload.STREAM_CODEC.encode(buffer, payload);
        final NanomachinePowerPayload decoded = NanomachinePowerPayload.STREAM_CODEC.decode(buffer);

        assertEquals(true, decoded.installed());
        assertEquals(12.5D, decoded.buffer());
        assertEquals(100D, decoded.maxBuffer());
        assertEquals(3, decoded.activeInputs());
        assertEquals(9, decoded.totalInputs());
        assertEquals(List.of("flame", "heart"), decoded.activeParticleEffects());
    }
}
