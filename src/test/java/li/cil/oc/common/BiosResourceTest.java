package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BiosResourceTest {
    @Test
    void bundledLuaBiosBootsFilesystemInit() {
        String bios = new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8);

        assertTrue(bios.contains("computer.getBootAddress"));
        assertTrue(bios.contains("/init.lua"));
        assertTrue(bios.contains("component.list(\"filesystem\")"));
    }
}
