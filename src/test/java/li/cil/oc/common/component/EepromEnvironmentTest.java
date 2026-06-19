package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
}
