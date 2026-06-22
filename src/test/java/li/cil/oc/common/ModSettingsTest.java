package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModSettingsTest {
    @Test
    void mfuSettingsExposeUpstreamDefaultsWhenConfigIsUnloaded() {
        assertEquals(3D, ModSettings.mfuRange());
        assertEquals(1D, ModSettings.mfuRelayCost());
        assertEquals(10, ModSettings.mfuTickFrequency());
        assertEquals(0.2D, ModSettings.solarGeneratorEfficiency());
        assertEquals(true, ModSettings.inputUsername());
        assertEquals(true, ModSettings.canComputersBeOwned());
        assertEquals(16, ModSettings.maxUsers());
        assertEquals(32, ModSettings.maxUsernameLength());
        assertEquals(5D, ModSettings.computerTimeout());
        assertEquals(false, ModSettings.allowBytecode());
        assertEquals(false, ModSettings.allowGc());
        assertEquals(8192, ModSettings.maxNetworkPacketSize());
        assertEquals(64, ModSettings.tmpSize());
        assertEquals(512, ModSettings.fileCost());
        assertEquals(512, ModSettings.floppySize());
        assertEquals(16, ModSettings.maxHandles());
        assertEquals(2048, ModSettings.maxReadBuffer());
        assertEquals(List.of(1024, 2048, 4096), ModSettings.hddSizes());
        assertEquals(1024, ModSettings.hddSize(0));
        assertEquals(2048, ModSettings.hddSize(1));
        assertEquals(4096, ModSettings.hddSize(2));
        assertEquals(1024, ModSettings.hddSize(-1));
        assertEquals(4096, ModSettings.hddSize(99));
    }

    @Test
    void mfuConfigUsesUpstreamCompatiblePaths() {
        assertEquals(List.of("misc", "mfuRange"), ModSettings.MFU_RANGE.getPath());
        assertEquals(List.of("power", "cost", "mfuRelay"), ModSettings.MFU_RELAY_COST.getPath());
        assertEquals(List.of("power", "tickFrequency"), ModSettings.MFU_TICK_FREQUENCY.getPath());
        assertEquals(List.of("power", "solarGeneratorEfficiency"), ModSettings.SOLAR_GENERATOR_EFFICIENCY.getPath());
        assertEquals(List.of("misc", "inputUsername"), ModSettings.INPUT_USERNAME.getPath());
        assertEquals(List.of("computer", "canComputersBeOwned"), ModSettings.CAN_COMPUTERS_BE_OWNED.getPath());
        assertEquals(List.of("computer", "maxUsers"), ModSettings.MAX_USERS.getPath());
        assertEquals(List.of("computer", "maxUsernameLength"), ModSettings.MAX_USERNAME_LENGTH.getPath());
        assertEquals(List.of("computer", "timeout"), ModSettings.COMPUTER_TIMEOUT.getPath());
        assertEquals(List.of("computer", "lua", "allowBytecode"), ModSettings.ALLOW_BYTECODE.getPath());
        assertEquals(List.of("computer", "lua", "allowGC"), ModSettings.ALLOW_GC.getPath());
        assertEquals(List.of("misc", "maxNetworkPacketSize"), ModSettings.MAX_NETWORK_PACKET_SIZE.getPath());
        assertEquals(List.of("filesystem", "tmpSize"), ModSettings.TMP_SIZE.getPath());
        assertEquals(List.of("filesystem", "fileCost"), ModSettings.FILE_COST.getPath());
        assertEquals(List.of("filesystem", "floppySize"), ModSettings.FLOPPY_SIZE.getPath());
        assertEquals(List.of("filesystem", "maxHandles"), ModSettings.MAX_HANDLES.getPath());
        assertEquals(List.of("filesystem", "maxReadBuffer"), ModSettings.MAX_READ_BUFFER.getPath());
        assertEquals(List.of("filesystem", "hddSizes"), ModSettings.HDD_SIZES.getPath());
    }
}
