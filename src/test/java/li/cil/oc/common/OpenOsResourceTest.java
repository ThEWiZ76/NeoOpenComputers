package li.cil.oc.common;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Mode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenOsResourceTest {
    @Test
    void bundledOpenOsFilesystemContainsBootFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "loot/openos");

        assertNotNull(fileSystem);
        assertTrue(fileSystem.isReadOnly());
        assertTrue(fileSystem.exists("init.lua"));
        assertTrue(fileSystem.exists("lib/core/boot.lua"));

        int handle = fileSystem.open("init.lua", Mode.Read);
        byte[] buffer = new byte[256];
        int read = fileSystem.getHandle(handle).read(buffer);
        fileSystem.getHandle(handle).close();

        String init = new String(buffer, 0, read, StandardCharsets.UTF_8);
        assertTrue(init.contains("computer.getBootAddress()"));
    }

    @Test
    void bundledOpenOsFilesystemIsExposedByLootDiskCatalog() {
        OpenComputersApi.initialize();

        assertNotNull(ModLootDisks.openOsFileSystem());
    }

    @Test
    void bundledLootDescriptorsIncludeOpenOs() {
        List<ModLootDisks.Descriptor> descriptors = ModLootDisks.bundledDescriptors();

        assertEquals(1, descriptors.size());
        ModLootDisks.Descriptor openOs = descriptors.getFirst();
        assertEquals("openos", openOs.path());
        assertEquals("OpenOS (Operating System)", openOs.label());
        assertEquals(0, openOs.weight());
        assertEquals(net.minecraft.world.item.DyeColor.GREEN, openOs.color());
    }
}
