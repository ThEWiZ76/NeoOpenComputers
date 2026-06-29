package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerminalFontShapeTest {
    @Test
    void texturedWorldGlyphVerticesIncludeLightmapUv2() throws IOException {
        final String font = Files.readString(Path.of("src/main/java/li/cil/oc/client/TerminalFont.java"));

        assertTrue(font.contains("RenderType.text(ASCII_GLYPH_TEXTURE)"));
        assertTrue(font.contains("texturedQuadVertex("));
        assertTrue(font.contains(".setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF)"),
            "Textured world glyphs must explicitly emit UV2 lightmap data for Sodium/Iris vertex validation.");
    }
}
