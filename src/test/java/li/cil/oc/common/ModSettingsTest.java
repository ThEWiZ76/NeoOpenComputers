package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class ModSettingsTest {
    @Test
    void mfuSettingsExposeUpstreamDefaultsWhenConfigIsUnloaded() {
        assertEquals(3D, ModSettings.mfuRange());
        assertEquals(1D, ModSettings.mfuRelayCost());
        assertEquals(10, ModSettings.mfuTickFrequency());
        assertEquals(0.2D, ModSettings.solarGeneratorEfficiency());
        assertEquals(0.8D, ModSettings.generatorEfficiency());
        assertEquals(List.of(10000D, 15000D, 20000D), ModSettings.batteryUpgradeBuffers());
        assertEquals(10000D, ModSettings.batteryUpgradeBuffer(0));
        assertEquals(15000D, ModSettings.batteryUpgradeBuffer(1));
        assertEquals(20000D, ModSettings.batteryUpgradeBuffer(2));
        assertEquals(10000D, ModSettings.batteryUpgradeBuffer(-1));
        assertEquals(20000D, ModSettings.batteryUpgradeBuffer(99));
        assertEquals(500D, ModSettings.powerDistributorBuffer());
        assertEquals(10000D, ModSettings.tabletBuffer());
        assertEquals(1000D, ModSettings.converterBuffer());
        assertEquals(500D, ModSettings.computerBuffer());
        assertEquals(true, ModSettings.inputUsername());
        assertEquals(25D, ModSettings.disassemblerTickAmount());
        assertEquals(2000D, ModSettings.disassemblerItemCost());
        assertEquals(true, ModSettings.canComputersBeOwned());
        assertEquals(16, ModSettings.maxUsers());
        assertEquals(32, ModSettings.maxUsernameLength());
        assertEquals(5D, ModSettings.computerTimeout());
        assertEquals(4096, ModSettings.eepromSize());
        assertEquals(256, ModSettings.eepromDataSize());
        assertEquals(List.of(8, 12, 16, 1024), ModSettings.cpuComponentCount());
        assertEquals(8, ModSettings.cpuComponentCount(0));
        assertEquals(12, ModSettings.cpuComponentCount(1));
        assertEquals(16, ModSettings.cpuComponentCount(2));
        assertEquals(1024, ModSettings.cpuComponentCount(3));
        assertEquals(8, ModSettings.cpuComponentCount(-1));
        assertEquals(1024, ModSettings.cpuComponentCount(99));
        assertEquals(List.of(0.5D, 1.0D, 1.5D), ModSettings.callBudgets());
        assertEquals(0.5D, ModSettings.callBudget(0));
        assertEquals(1.0D, ModSettings.callBudget(1));
        assertEquals(1.5D, ModSettings.callBudget(2));
        assertEquals(0.5D, ModSettings.callBudget(-1));
        assertEquals(1.5D, ModSettings.callBudget(99));
        assertEquals(false, ModSettings.eraseTmpOnReboot());
        assertEquals(12, ModSettings.executionDelay());
        assertEquals(false, ModSettings.allowBytecode());
        assertEquals(false, ModSettings.allowGc());
        assertEquals(5, ModSettings.initialNetworkPacketTtl());
        assertEquals(8192, ModSettings.maxNetworkPacketSize());
        assertEquals(8, ModSettings.maxNetworkPacketParts());
        assertEquals(0.05D, ModSettings.disassemblerBreakChance());
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
        assertEquals(true, ModSettings.enableHttp());
        assertEquals(true, ModSettings.enableHttpHeaders());
        assertEquals(true, ModSettings.enableTcp());
        assertEquals(List.of("removeme", "deny private", "deny bogon", "allow default"), ModSettings.internetFilteringRules());
        assertEquals(0, ModSettings.httpRequestTimeout());
        assertEquals(4, ModSettings.internetThreads());
        assertEquals(4, ModSettings.maxTcpConnections());
        assertEquals("opencomputers/" + API.VERSION, ModSettings.httpUserAgent());
        assertEquals(List.of(3D, 4D), ModSettings.hologramMaxScale());
        assertEquals(3D, ModSettings.hologramMaxScale(0));
        assertEquals(4D, ModSettings.hologramMaxScale(1));
        assertEquals(3D, ModSettings.hologramMaxScale(-1));
        assertEquals(4D, ModSettings.hologramMaxScale(99));
        assertEquals(List.of(1D, 2D), ModSettings.hologramMaxTranslation());
        assertEquals(1D, ModSettings.hologramMaxTranslation(0));
        assertEquals(2D, ModSettings.hologramMaxTranslation(1));
        assertEquals(1D, ModSettings.hologramMaxTranslation(-1));
        assertEquals(2D, ModSettings.hologramMaxTranslation(99));
        assertEquals(0.2D, ModSettings.hologramSetRawDelay());
        assertEquals(0.1D / 1024.0D, ModSettings.hddReadCost(), 0.000_001D);
        assertEquals(0.25D / 1024.0D, ModSettings.hddWriteCost(), 0.000_001D);
        assertEquals(List.of(1024, 2048, 4096), ModSettings.hddSizes());
        assertEquals(1024, ModSettings.hddSize(0));
        assertEquals(2048, ModSettings.hddSize(1));
        assertEquals(4096, ModSettings.hddSize(2));
        assertEquals(1024, ModSettings.hddSize(-1));
        assertEquals(4096, ModSettings.hddSize(99));
        assertEquals(List.of(2, 4, 8), ModSettings.hddPlatterCounts());
        assertEquals(2, ModSettings.hddPlatterCount(0));
        assertEquals(4, ModSettings.hddPlatterCount(1));
        assertEquals(8, ModSettings.hddPlatterCount(2));
        assertEquals(2, ModSettings.hddPlatterCount(-1));
        assertEquals(8, ModSettings.hddPlatterCount(99));
        assertEquals(100_000D, ModSettings.nanomachinesBuffer());
        assertEquals(0.4D, ModSettings.nanomachineTriggerQuota());
        assertEquals(0.2D, ModSettings.nanomachineConnectorQuota());
        assertEquals(2, ModSettings.nanomachineMaxInputs());
        assertEquals(2, ModSettings.nanomachineMaxOutputs());
        assertEquals(2, ModSettings.nanomachinesSafeInputsActive());
        assertEquals(4, ModSettings.nanomachinesMaxInputsActive());
        assertEquals(1D, ModSettings.nanomachinesCommandDelay());
        assertEquals(2D, ModSettings.nanomachinesCommandRange());
        assertEquals(5D, ModSettings.nanomachinesHungryDamage());
        assertEquals(50D, ModSettings.nanomachinesHungryEnergyRestored());
        assertEquals(8D, ModSettings.nanomachinesMagnetRange());
        assertEquals(1D, ModSettings.nanomachinesDisintegrationRange());
        assertEquals(List.of(
            "speed",
            "haste",
            "strength",
            "jump_boost",
            "resistance",
            "fire_resistance",
            "water_breathing",
            "night_vision",
            "absorption",
            "blindness",
            "nausea",
            "mining_fatigue",
            "instant_damage",
            "hunger",
            "slowness",
            "poison",
            "weakness",
            "wither"), ModSettings.nanomachinesPotionWhitelist());
    }

    @Test
    void mfuConfigUsesUpstreamCompatiblePaths() {
        assertEquals(List.of("misc", "mfuRange"), ModSettings.MFU_RANGE.getPath());
        assertEquals(List.of("power", "cost", "mfuRelay"), ModSettings.MFU_RELAY_COST.getPath());
        assertEquals(List.of("power", "ignorePower"), ModSettings.IGNORE_POWER.getPath());
        assertEquals(List.of("power", "tickFrequency"), ModSettings.MFU_TICK_FREQUENCY.getPath());
        assertEquals(List.of("power", "solarGeneratorEfficiency"), ModSettings.SOLAR_GENERATOR_EFFICIENCY.getPath());
        assertEquals(List.of("power", "generatorEfficiency"), ModSettings.GENERATOR_EFFICIENCY.getPath());
        assertEquals(List.of("power", "buffer", "batteryUpgrades"), ModSettings.BATTERY_UPGRADE_BUFFERS.getPath());
        assertEquals(List.of("power", "buffer", "distributor"), ModSettings.POWER_DISTRIBUTOR_BUFFER.getPath());
        assertEquals(List.of("power", "buffer", "tablet"), ModSettings.TABLET_BUFFER.getPath());
        assertEquals(List.of("power", "buffer", "converter"), ModSettings.CONVERTER_BUFFER.getPath());
        assertEquals(List.of("power", "buffer", "computer"), ModSettings.COMPUTER_BUFFER.getPath());
        assertEquals(List.of("power", "disassemblerTickAmount"), ModSettings.DISASSEMBLER_TICK_AMOUNT.getPath());
        assertEquals(List.of("power", "cost", "disassemblerPerItem"), ModSettings.DISASSEMBLER_ITEM_COST.getPath());
        assertEquals(List.of("misc", "inputUsername"), ModSettings.INPUT_USERNAME.getPath());
        assertEquals(List.of("computer", "canComputersBeOwned"), ModSettings.CAN_COMPUTERS_BE_OWNED.getPath());
        assertEquals(List.of("computer", "maxUsers"), ModSettings.MAX_USERS.getPath());
        assertEquals(List.of("computer", "maxUsernameLength"), ModSettings.MAX_USERNAME_LENGTH.getPath());
        assertEquals(List.of("computer", "timeout"), ModSettings.COMPUTER_TIMEOUT.getPath());
        assertEquals(List.of("computer", "eepromSize"), ModSettings.EEPROM_SIZE.getPath());
        assertEquals(List.of("computer", "eepromDataSize"), ModSettings.EEPROM_DATA_SIZE.getPath());
        assertEquals(List.of("computer", "cpuComponentCount"), ModSettings.CPU_COMPONENT_COUNT.getPath());
        assertEquals(List.of("computer", "callBudgets"), ModSettings.CALL_BUDGETS.getPath());
        assertEquals(List.of("computer", "eraseTmpOnReboot"), ModSettings.ERASE_TMP_ON_REBOOT.getPath());
        assertEquals(List.of("computer", "executionDelay"), ModSettings.EXECUTION_DELAY.getPath());
        assertEquals(List.of("computer", "lua", "allowBytecode"), ModSettings.ALLOW_BYTECODE.getPath());
        assertEquals(List.of("computer", "lua", "allowGC"), ModSettings.ALLOW_GC.getPath());
        assertEquals(List.of("misc", "initialNetworkPacketTTL"), ModSettings.INITIAL_NETWORK_PACKET_TTL.getPath());
        assertEquals(List.of("misc", "maxNetworkPacketSize"), ModSettings.MAX_NETWORK_PACKET_SIZE.getPath());
        assertEquals(List.of("misc", "maxNetworkPacketParts"), ModSettings.MAX_NETWORK_PACKET_PARTS.getPath());
        assertEquals(List.of("misc", "disassemblerBreakChance"), ModSettings.DISASSEMBLER_BREAK_CHANCE.getPath());
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
        assertEquals(List.of("filesystem", "hddRead"), ModSettings.HDD_READ.getPath());
        assertEquals(List.of("filesystem", "hddWrite"), ModSettings.HDD_WRITE.getPath());
        assertEquals(List.of("filesystem", "maxHandles"), ModSettings.MAX_HANDLES.getPath());
        assertEquals(List.of("filesystem", "maxReadBuffer"), ModSettings.MAX_READ_BUFFER.getPath());
        assertEquals(List.of("filesystem", "hddSizes"), ModSettings.HDD_SIZES.getPath());
        assertEquals(List.of("filesystem", "hddPlatterCounts"), ModSettings.HDD_PLATTER_COUNTS.getPath());
        assertEquals(List.of("internet", "enableHttp"), ModSettings.ENABLE_HTTP.getPath());
        assertEquals(List.of("internet", "enableHttpHeaders"), ModSettings.ENABLE_HTTP_HEADERS.getPath());
        assertEquals(List.of("internet", "enableTcp"), ModSettings.ENABLE_TCP.getPath());
        assertEquals(List.of("internet", "filteringRules"), ModSettings.FILTERING_RULES.getPath());
        assertEquals(List.of("internet", "requestTimeout"), ModSettings.REQUEST_TIMEOUT.getPath());
        assertEquals(List.of("internet", "threads"), ModSettings.INTERNET_THREADS.getPath());
        assertEquals(List.of("internet", "maxTcpConnections"), ModSettings.MAX_TCP_CONNECTIONS.getPath());
        assertEquals(List.of("internet", "httpUserAgent"), ModSettings.HTTP_USER_AGENT.getPath());
        assertEquals(List.of("client", "enableNanomachinePfx"), ModSettings.ENABLE_NANOMACHINE_PFX.getPath());
        assertEquals(List.of("client", "nanomachineHudPos"), ModSettings.NANOMACHINE_HUD_POS.getPath());
        assertEquals(List.of("hologram", "maxScale"), ModSettings.HOLOGRAM_MAX_SCALE.getPath());
        assertEquals(List.of("hologram", "maxTranslation"), ModSettings.HOLOGRAM_MAX_TRANSLATION.getPath());
        assertEquals(List.of("hologram", "setRawDelay"), ModSettings.HOLOGRAM_SET_RAW_DELAY.getPath());
        assertEquals(List.of("power", "cost", "hologram"), ModSettings.HOLOGRAM_COST.getPath());
        assertEquals(List.of("power", "buffer", "nanomachines"), ModSettings.NANOMACHINES_BUFFER.getPath());
        assertEquals(List.of("power", "cost", "nanomachineInput"), ModSettings.NANOMACHINES_INPUT_COST.getPath());
        assertEquals(List.of("power", "cost", "nanomachinesReconfigure"), ModSettings.NANOMACHINES_RECONFIGURE_COST.getPath());
        assertEquals(List.of("nanomachines", "triggerQuota"), ModSettings.NANOMACHINES_TRIGGER_QUOTA.getPath());
        assertEquals(List.of("nanomachines", "connectorQuota"), ModSettings.NANOMACHINES_CONNECTOR_QUOTA.getPath());
        assertEquals(List.of("nanomachines", "maxInputs"), ModSettings.NANOMACHINE_MAX_INPUTS.getPath());
        assertEquals(List.of("nanomachines", "maxOutputs"), ModSettings.NANOMACHINE_MAX_OUTPUTS.getPath());
        assertEquals(List.of("nanomachines", "safeInputsActive"), ModSettings.NANOMACHINES_SAFE_INPUTS_ACTIVE.getPath());
        assertEquals(List.of("nanomachines", "maxInputsActive"), ModSettings.NANOMACHINES_MAX_INPUTS_ACTIVE.getPath());
        assertEquals(List.of("nanomachines", "commandDelay"), ModSettings.NANOMACHINES_COMMAND_DELAY.getPath());
        assertEquals(List.of("nanomachines", "commandRange"), ModSettings.NANOMACHINES_COMMAND_RANGE.getPath());
        assertEquals(List.of("nanomachines", "hungryDamage"), ModSettings.NANOMACHINES_HUNGRY_DAMAGE.getPath());
        assertEquals(List.of("nanomachines", "hungryEnergyRestored"), ModSettings.NANOMACHINES_HUNGRY_ENERGY_RESTORED.getPath());
        assertEquals(List.of("nanomachines", "magnetRange"), ModSettings.NANOMACHINES_MAGNET_RANGE.getPath());
        assertEquals(List.of("nanomachines", "disintegrationRange"), ModSettings.NANOMACHINES_DISINTEGRATION_RANGE.getPath());
        assertEquals(List.of("nanomachines", "potionWhitelist"), ModSettings.NANOMACHINES_POTION_WHITELIST.getPath());
    }

    @Test
    void nanomachinesControllerReadsConfiguredLimitsAndBuffer() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_BUFFER, 42D, () ->
            withCachedConfig(ModSettings.NANOMACHINES_INPUT_COST, 0.25D, () ->
            withCachedConfig(ModSettings.NANOMACHINES_RECONFIGURE_COST, 0.75D, () ->
                withCachedConfig(ModSettings.NANOMACHINES_SAFE_INPUTS_ACTIVE, 1, () ->
                    withCachedConfig(ModSettings.NANOMACHINES_MAX_INPUTS_ACTIVE, 3, () -> {
                        final SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());

                        assertEquals(42D, controller.getLocalBufferSize());
                        assertEquals(10.5D, controller.getLocalBuffer());
                        assertEquals(0.25D, ModSettings.nanomachinesInputCost());
                        assertEquals(0.75D, ModSettings.nanomachinesReconfigureCost());
                        assertEquals(1, controller.getSafeActiveInputs());
                        assertEquals(3, controller.getMaxActiveInputs());
                    })))));
    }

    @Test
    void nanomachinesControllerUsesConfiguredTriggerQuotaForInputCount() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_TRIGGER_QUOTA, 2D, () -> {
            final NanomachinesRegistry registry = new NanomachinesRegistry();
            registry.addProvider(new TestBehaviorProvider(3));

            final SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);

            assertEquals(6, controller.getTotalInputCount());
        });
    }

    @Test
    void nanomachinesVisualSettingsReadClientConfiguration() throws Exception {
        withCachedConfig(ModSettings.ENABLE_NANOMACHINE_PFX, false, () ->
            withCachedConfig(ModSettings.NANOMACHINE_HUD_POS, List.of(0.5D, 16D), () -> {
                assertFalse(ModSettings.enableNanomachinePfx());
                assertEquals(List.of(0.5D, 16D), ModSettings.nanomachineHudPos());
            }));
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record TestBehaviorProvider(int count) implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
                .toList();
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return new TestBehavior("loaded");
        }
    }

    private record TestBehavior(String name) implements Behavior {
        @Override
        public String getNameHint() {
            return name;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
        }

        @Override
        public void update() {
        }
    }
}
