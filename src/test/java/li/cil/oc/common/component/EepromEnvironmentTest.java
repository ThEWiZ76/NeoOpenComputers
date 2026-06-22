package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EepromEnvironmentTest {
    @AfterEach
    void resetApi() {
        li.cil.oc.api.API.network = null;
    }

    @Test
    void createsEepromComponentNode() {
        OpenComputersApi.initialize();
        EepromEnvironment environment = new EepromEnvironment(data("Lua BIOS", "code", "data", false));

        Component component = assertInstanceOf(Component.class, environment.node());
        assertEquals("eeprom", component.name());
        assertEquals(Visibility.Neighbors, component.visibility());
        assertTrue(component.methods().contains("get"));
        assertTrue(component.methods().contains("setData"));
    }

    @Test
    void readsAndWritesEepromData() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag data = data("Lua BIOS", "code", "data", false);
        Component component = (Component) new EepromEnvironment(data).node();

        assertArrayEquals(bytes("code"), (byte[]) component.invoke("get", null)[0]);
        assertArrayEquals(bytes("data"), (byte[]) component.invoke("getData", null)[0]);
        assertArrayEquals(new Object[]{"Lua BIOS"}, component.invoke("getLabel", null));
        assertArrayEquals(new Object[]{4096}, component.invoke("getSize", null));
        assertArrayEquals(new Object[]{256}, component.invoke("getDataSize", null));

        component.invoke("set", null, "next");
        component.invoke("setData", null, "user");
        assertArrayEquals(new Object[]{"Changed"}, component.invoke("setLabel", null, "Changed"));

        assertArrayEquals(bytes("next"), data.getByteArray(ItemRegistry.EEPROM_CODE_TAG));
        assertArrayEquals(bytes("user"), data.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG));
        assertEquals("Changed", data.getString(ItemRegistry.EEPROM_LABEL_TAG));
    }

    @Test
    void readonlyEepromRejectsCodeAndLabelWrites() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag data = data("ROM", "code", "data", true);
        Component component = (Component) new EepromEnvironment(data).node();

        assertArrayEquals(new Object[]{null, "storage is readonly"}, component.invoke("set", null, "next"));
        assertArrayEquals(new Object[]{null, "storage is readonly"}, component.invoke("setLabel", null, "Changed"));
        assertArrayEquals(bytes("code"), (byte[]) component.invoke("get", null)[0]);
        assertArrayEquals(new Object[]{"ROM"}, component.invoke("getLabel", null));
    }

    @Test
    void eepromWritesConsumeEnergyAndPauseLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag data = data("Lua BIOS", "code", "data", false);
        Component component = (Component) new EepromEnvironment(data).node();
        Connector connector = connectorWithEnergy(100);
        RecordingContext context = new RecordingContext(connector);

        component.invoke("set", context, "next");
        assertEquals(50, connector.localBuffer());
        assertEquals(2, context.pauseSeconds);

        context.pauseSeconds = -1;
        component.invoke("setData", context, "user");
        assertEquals(0, connector.localBuffer());
        assertEquals(1, context.pauseSeconds);
    }

    @Test
    void eepromWritesFailWithoutEnoughEnergy() throws Exception {
        OpenComputersApi.initialize();
        CompoundTag data = data("Lua BIOS", "code", "data", false);
        Component component = (Component) new EepromEnvironment(data).node();
        Connector connector = connectorWithEnergy(49);
        RecordingContext context = new RecordingContext(connector);

        assertArrayEquals(new Object[]{null, "not enough energy"}, component.invoke("set", context, "next"));
        assertArrayEquals(bytes("code"), data.getByteArray(ItemRegistry.EEPROM_CODE_TAG));
        assertEquals(-1, context.pauseSeconds);
    }

    @Test
    void exposesDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        EepromEnvironment environment = new EepromEnvironment(data("ROM", "code", "data", false));

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, environment);
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Memory, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("EEPROM", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("FlashStick2k", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("4096", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("4096", metadata.get(DeviceInfo.DeviceAttribute.Size));
    }

    @Test
    void usesConfiguredCodeAndDataSizes() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.EEPROM_SIZE, 6, () ->
            withCachedConfig(ModSettings.EEPROM_DATA_SIZE, 3, () -> {
                CompoundTag data = data("ROM", "code", "dat", false);
                Component component = (Component) new EepromEnvironment(data).node();
                DeviceInfo info = assertInstanceOf(DeviceInfo.class, component.host());

                assertArrayEquals(new Object[]{6}, component.invoke("getSize", null));
                assertArrayEquals(new Object[]{3}, component.invoke("getDataSize", null));
                assertEquals("6", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
                assertEquals("6", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Size));

                component.invoke("set", null, "123456");
                component.invoke("setData", null, "123");

                assertThrows(IllegalArgumentException.class, () -> component.invoke("set", null, "1234567"));
                assertThrows(IllegalArgumentException.class, () -> component.invoke("setData", null, "1234"));
            }));
    }

    private static CompoundTag data(final String label, final String code, final String data, final boolean readonly) {
        CompoundTag tag = new CompoundTag();
        tag.putString(ItemRegistry.EEPROM_LABEL_TAG, label);
        tag.putByteArray(ItemRegistry.EEPROM_CODE_TAG, bytes(code));
        tag.putByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG, bytes(data));
        tag.putBoolean(ItemRegistry.EEPROM_READONLY_TAG, readonly);
        return tag;
    }

    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Connector connectorWithEnergy(final double energy) {
        Connector connector = li.cil.oc.api.API.network
            .newNode(new AbstractManagedEnvironment() {
            }, Visibility.Network)
            .withConnector(100)
            .create();
        connector.changeBuffer(energy);
        return connector;
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingContext implements Context {
        private final Node node;
        private double pauseSeconds = -1;

        private RecordingContext(final Node node) {
            this.node = node;
        }

        @Override public Node node() { return node; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { pauseSeconds = seconds; return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { }
        @Override public boolean signal(final String name, final Object... args) { return true; }
    }
}
