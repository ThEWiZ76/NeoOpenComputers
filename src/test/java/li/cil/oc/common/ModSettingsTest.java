package li.cil.oc.common;

import li.cil.oc.api.API;
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
        assertEquals(5, ModSettings.initialNetworkPacketTtl());
        assertEquals(8192, ModSettings.maxNetworkPacketSize());
        assertEquals(8, ModSettings.maxNetworkPacketParts());
        assertEquals(5, ModSettings.defaultRelayDelay());
        assertEquals(1.5D, ModSettings.relayDelayUpgrade());
        assertEquals(20, ModSettings.defaultMaxQueueSize());
        assertEquals(10, ModSettings.queueSizeUpgrade());
        assertEquals(1, ModSettings.defaultRelayAmount());
        assertEquals(1, ModSettings.relayAmountUpgrade());
        assertEquals(List.of(16, 1, 16), ModSettings.maxOpenPorts());
        assertEquals(16, ModSettings.maxOpenPorts(0));
        assertEquals(1, ModSettings.maxOpenPorts(1));
        assertEquals(16, ModSettings.maxOpenPorts(2));
        assertEquals(16, ModSettings.maxOpenPorts(-1));
        assertEquals(16, ModSettings.maxOpenPorts(99));
        assertEquals(List.of(16D, 400D), ModSettings.maxWirelessRange());
        assertEquals(16D, ModSettings.maxWirelessRange(0));
        assertEquals(400D, ModSettings.maxWirelessRange(1));
        assertEquals(16D, ModSettings.maxWirelessRange(-1));
        assertEquals(400D, ModSettings.maxWirelessRange(99));
        assertEquals(List.of(0.05D, 0.05D), ModSettings.wirelessCostPerRange());
        assertEquals(0.05D, ModSettings.wirelessCostPerRange(0));
        assertEquals(0.05D, ModSettings.wirelessCostPerRange(1));
        assertEquals(64, ModSettings.tmpSize());
        assertEquals(512, ModSettings.fileCost());
        assertEquals(512, ModSettings.floppySize());
        assertEquals(16, ModSettings.maxHandles());
        assertEquals(2048, ModSettings.maxReadBuffer());
        assertEquals(4, ModSettings.maxTcpConnections());
        assertEquals("opencomputers/" + API.VERSION, ModSettings.httpUserAgent());
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
        assertEquals(List.of("misc", "initialNetworkPacketTTL"), ModSettings.INITIAL_NETWORK_PACKET_TTL.getPath());
        assertEquals(List.of("misc", "maxNetworkPacketSize"), ModSettings.MAX_NETWORK_PACKET_SIZE.getPath());
        assertEquals(List.of("misc", "maxNetworkPacketParts"), ModSettings.MAX_NETWORK_PACKET_PARTS.getPath());
        assertEquals(List.of("misc", "defaultRelayDelay"), ModSettings.DEFAULT_RELAY_DELAY.getPath());
        assertEquals(List.of("misc", "relayDelayUpgrade"), ModSettings.RELAY_DELAY_UPGRADE.getPath());
        assertEquals(List.of("misc", "defaultMaxQueueSize"), ModSettings.DEFAULT_MAX_QUEUE_SIZE.getPath());
        assertEquals(List.of("misc", "queueSizeUpgrade"), ModSettings.QUEUE_SIZE_UPGRADE.getPath());
        assertEquals(List.of("misc", "defaultRelayAmount"), ModSettings.DEFAULT_RELAY_AMOUNT.getPath());
        assertEquals(List.of("misc", "relayAmountUpgrade"), ModSettings.RELAY_AMOUNT_UPGRADE.getPath());
        assertEquals(List.of("misc", "maxOpenPorts"), ModSettings.MAX_OPEN_PORTS.getPath());
        assertEquals(List.of("misc", "maxWirelessRange"), ModSettings.MAX_WIRELESS_RANGE.getPath());
        assertEquals(List.of("power", "cost", "wirelessCostPerRange"), ModSettings.WIRELESS_COST_PER_RANGE.getPath());
        assertEquals(List.of("filesystem", "tmpSize"), ModSettings.TMP_SIZE.getPath());
        assertEquals(List.of("filesystem", "fileCost"), ModSettings.FILE_COST.getPath());
        assertEquals(List.of("filesystem", "floppySize"), ModSettings.FLOPPY_SIZE.getPath());
        assertEquals(List.of("filesystem", "maxHandles"), ModSettings.MAX_HANDLES.getPath());
        assertEquals(List.of("filesystem", "maxReadBuffer"), ModSettings.MAX_READ_BUFFER.getPath());
        assertEquals(List.of("filesystem", "hddSizes"), ModSettings.HDD_SIZES.getPath());
        assertEquals(List.of("internet", "maxTcpConnections"), ModSettings.MAX_TCP_CONNECTIONS.getPath());
        assertEquals(List.of("internet", "httpUserAgent"), ModSettings.HTTP_USER_AGENT.getPath());
    }
}
