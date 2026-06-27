package li.cil.oc.common.item;

import li.cil.oc.common.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FloppyItemColorTest {
    @Test
    void floppyColorIndexUsesUpstreamDyeOrderAndGrayDefault() {
        assertEquals(8, FloppyItem.floppyColorIndex(new CompoundTag()));
        assertEquals(2, FloppyItem.floppyColorIndex(tagWithColor("green")));
        assertEquals(7, FloppyItem.floppyColorIndex(tagWithColor("light_gray")));
        assertEquals(12, FloppyItem.floppyColorIndex(tagWithColor("light_blue")));
        assertEquals(15, FloppyItem.floppyColorIndex(tagWithColor("white")));
    }

    @Test
    void floppyColorIndexSupportsLegacyIntegerColorTag() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(ItemRegistry.FLOPPY_COLOR_TAG, 3);

        assertEquals(3, FloppyItem.floppyColorIndex(tag));
    }

    private static CompoundTag tagWithColor(final String colorName) {
        final CompoundTag tag = new CompoundTag();
        tag.putString(ItemRegistry.FLOPPY_COLOR_TAG, colorName);
        return tag;
    }
}
