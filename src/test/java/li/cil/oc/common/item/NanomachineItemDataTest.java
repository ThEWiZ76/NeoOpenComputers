package li.cil.oc.common.item;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachineItemDataTest {
    @Test
    void storesUuidAndConfigurationUnderUpstreamTags() {
        final CompoundTag root = new CompoundTag();
        final CompoundTag configuration = new CompoundTag();
        configuration.putInt("marker", 42);

        NanomachineItemData.save(root, "controller-id", configuration);

        assertEquals("controller-id", NanomachineItemData.uuid(root));
        assertTrue(NanomachineItemData.hasConfiguration(root));
        assertEquals(42, NanomachineItemData.configuration(root).getInt("marker"));
        assertEquals("controller-id", root.getString("oc:uuid"));
        assertTrue(root.contains("oc:configuration", CompoundTag.TAG_COMPOUND));
    }

    @Test
    void blankDataHasNoConfiguration() {
        final CompoundTag root = new CompoundTag();

        assertEquals("", NanomachineItemData.uuid(root));
        assertFalse(NanomachineItemData.hasConfiguration(root));
        assertTrue(NanomachineItemData.configuration(root).isEmpty());
    }
}
