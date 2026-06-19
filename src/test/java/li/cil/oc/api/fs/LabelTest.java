package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LabelTest {
    @Test
    void exposesMutablePersistableFileSystemLabel() {
        Label label = new TestLabel();
        CompoundTag tag = new CompoundTag();

        label.setLabel("disk");
        label.save(tag);
        label.setLabel(null);
        label.load(tag);

        assertTrue(label instanceof Persistable);
        assertEquals("disk", label.getLabel());
    }

    private static final class TestLabel implements Label {
        private String value;

        @Override
        public String getLabel() {
            return value;
        }

        @Override
        public void setLabel(final String value) {
            this.value = value;
        }

        @Override
        public void load(final CompoundTag nbt) {
            value = nbt.getString("label");
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putString("label", value);
        }
    }
}
