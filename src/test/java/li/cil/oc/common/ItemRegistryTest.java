package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ItemRegistryTest {
    @AfterEach
    void resetApi() {
        API.items = null;
    }

    @Test
    void bootstrapInstallsItemApi() {
        OpenComputersApi.initialize();

        assertInstanceOf(ItemRegistry.class, API.items);
    }

    @Test
    void registeredInfosAreAvailableByName() {
        ItemRegistry registry = new ItemRegistry();
        ItemInfo info = registry.register("computer.case1", null, null);

        assertSame(info, registry.get("computer.case1"));
        assertEquals("computer.case1", info.name());
        assertNull(info.block());
        assertNull(info.item());
        assertNull(info.createItemStack(1));
    }

    @Test
    void missingItemsAndDeferredFactoriesReturnNull() {
        ItemRegistry registry = new ItemRegistry();

        assertNull(registry.get("missing"));
        assertNull(registry.get((ItemStack) null));
        assertNull(registry.registerFloppy("loot", DyeColor.BLUE, () -> null, true));
        assertNull(registry.registerEEPROM("bios", new byte[]{1}, new byte[]{2}, true));
    }

    @Test
    void eepromFactoryDataStoresConfiguredCodeDataAndReadonlyFlag() {
        CompoundTag data = ItemRegistry.createEepromData("bios", new byte[]{1, 2}, new byte[]{3}, true);

        assertEquals("bios", data.getString(ItemRegistry.EEPROM_LABEL_TAG));
        assertArrayEquals(new byte[]{1, 2}, data.getByteArray(ItemRegistry.EEPROM_CODE_TAG));
        assertArrayEquals(new byte[]{3}, data.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG));
        assertEquals(true, data.getBoolean(ItemRegistry.EEPROM_READONLY_TAG));
    }
}
