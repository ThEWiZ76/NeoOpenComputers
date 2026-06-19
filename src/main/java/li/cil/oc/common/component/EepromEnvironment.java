package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;

public final class EepromEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final int EEPROM_SIZE = 4096;
    private static final int DATA_SIZE = 256;
    private static final int MAX_LABEL_LENGTH = 24;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Memory,
        DeviceInfo.DeviceAttribute.Description, "EEPROM",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "FlashStick2k",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(EEPROM_SIZE),
        DeviceInfo.DeviceAttribute.Size, Integer.toString(EEPROM_SIZE)
    );

    private final CompoundTag data;
    private final Runnable onChanged;

    public EepromEnvironment(final CompoundTag data) {
        this(data, () -> {
        });
    }

    public EepromEnvironment(final CompoundTag data, final Runnable onChanged) {
        this.data = data == null ? new CompoundTag() : data;
        this.onChanged = onChanged == null ? () -> {
        } : onChanged;
        setNode(Network.newNode(this, Visibility.Neighbors)
            .withComponent("eeprom", Visibility.Neighbors)
            .withConnector()
            .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] get(final Context context, final Arguments arguments) {
        return new Object[]{copyBytes(data.getByteArray(ItemRegistry.EEPROM_CODE_TAG))};
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] set(final Context context, final Arguments arguments) {
        if (data.getBoolean(ItemRegistry.EEPROM_READONLY_TAG)) {
            return new Object[]{null, "storage is readonly"};
        }
        final byte[] newData = arguments.optByteArray(0, new byte[0]);
        if (newData.length > EEPROM_SIZE) {
            throw new IllegalArgumentException("not enough space");
        }
        data.putByteArray(ItemRegistry.EEPROM_CODE_TAG, copyBytes(newData));
        onChanged.run();
        return null;
    }

    @Callback(direct = true, doc = "function():string -- Get the label of the EEPROM.")
    public Object[] getLabel(final Context context, final Arguments arguments) {
        return new Object[]{label()};
    }

    @Callback(doc = "function(data:string):string -- Set the label of the EEPROM.")
    public Object[] setLabel(final Context context, final Arguments arguments) {
        if (data.getBoolean(ItemRegistry.EEPROM_READONLY_TAG)) {
            return new Object[]{null, "storage is readonly"};
        }
        String label = arguments.optString(0, "EEPROM").trim();
        if (label.length() > MAX_LABEL_LENGTH) {
            label = label.substring(0, MAX_LABEL_LENGTH);
        }
        if (label.isEmpty()) {
            label = "EEPROM";
        }
        data.putString(ItemRegistry.EEPROM_LABEL_TAG, label);
        onChanged.run();
        return new Object[]{label};
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getSize(final Context context, final Arguments arguments) {
        return new Object[]{EEPROM_SIZE};
    }

    @Callback(direct = true, doc = "function():string -- Get the checksum of the data on this EEPROM.")
    public Object[] getChecksum(final Context context, final Arguments arguments) {
        final CRC32 crc32 = new CRC32();
        crc32.update(data.getByteArray(ItemRegistry.EEPROM_CODE_TAG));
        return new Object[]{String.format("%08x", crc32.getValue())};
    }

    @Callback(direct = true, doc = "function(checksum:string):boolean -- Make this EEPROM readonly if it isn't already.")
    public Object[] makeReadonly(final Context context, final Arguments arguments) {
        if (arguments.checkString(0).equals(getChecksum(context, arguments)[0])) {
            data.putBoolean(ItemRegistry.EEPROM_READONLY_TAG, true);
            onChanged.run();
            return new Object[]{true};
        }
        return new Object[]{null, "incorrect checksum"};
    }

    @Callback(direct = true, doc = "function():number -- Get the storage capacity of this EEPROM.")
    public Object[] getDataSize(final Context context, final Arguments arguments) {
        return new Object[]{DATA_SIZE};
    }

    @Callback(direct = true, doc = "function():string -- Get the currently stored byte array.")
    public Object[] getData(final Context context, final Arguments arguments) {
        return new Object[]{copyBytes(data.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG))};
    }

    @Callback(doc = "function(data:string) -- Overwrite the currently stored byte array.")
    public Object[] setData(final Context context, final Arguments arguments) {
        final byte[] newData = arguments.optByteArray(0, new byte[0]);
        if (newData.length > DATA_SIZE) {
            throw new IllegalArgumentException("not enough space");
        }
        data.putByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG, copyBytes(newData));
        onChanged.run();
        return null;
    }

    private String label() {
        return data.contains(ItemRegistry.EEPROM_LABEL_TAG) ? data.getString(ItemRegistry.EEPROM_LABEL_TAG) : "EEPROM";
    }

    private static byte[] copyBytes(final byte[] bytes) {
        return bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
    }
}
