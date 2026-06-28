package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModCreativeTabsSourceTest {
    private static final Path SOURCE = Path.of("src/main/java/li/cil/oc/common/ModCreativeTabs.java");

    @Test
    void bootableRegisteredStacksAppearBeforeRawTabItems() throws IOException {
        final String source = Files.readString(SOURCE);
        final int registeredStacks = source.indexOf("addRegisteredStacks(output)");
        final int registeredItems = source.indexOf("addRegisteredItems(output)");

        assertTrue(registeredStacks >= 0, "Creative tab should add registered bootable stacks explicitly");
        assertTrue(registeredItems >= 0, "Creative tab should add normal registered items explicitly");
        assertTrue(registeredStacks < registeredItems,
            "Bootable Lua BIOS/OpenOS stacks should appear before raw blank EEPROM/floppy items");
    }
}
