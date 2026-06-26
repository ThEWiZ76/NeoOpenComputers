package li.cil.oc.common.item;

import li.cil.oc.api.fs.Label;
import net.minecraft.nbt.CompoundTag;

final class ItemDiskLabel implements Label {
    static final String TAG = "oc:fs.label";
    private static final int MAX_LENGTH = 16;

    private String label;

    ItemDiskLabel(final String label) {
        setLabel(label);
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(final String value) {
        if (value == null) {
            label = null;
        } else {
            label = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG)) {
            setLabel(nbt.getString(TAG));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (label != null) {
            nbt.putString(TAG, label);
        }
    }
}
