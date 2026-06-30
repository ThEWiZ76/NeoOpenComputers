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
        assertTrue(font.contains("texturedQuad(consumer, pose"));
        assertTrue(font.contains(".setUv(u0, v1).setUv2(WORLD_GLYPH_LIGHT"),
            "World ASCII glyphs must render as textured quads with UV2 lightmap data.");
        assertTrue(font.contains("quad(consumer, pose, baseX + px, baseY + py, z, color)"),
            "Non-atlas glyph fallback should keep direct terminal-cell raster rendering.");
        assertTrue(font.contains(".setUv2(WORLD_GLYPH_LIGHT & 0xFFFF, WORLD_GLYPH_LIGHT >> 16 & 0xFFFF)"),
            "World glyphs must explicitly emit UV2 lightmap data for Sodium/Iris vertex validation.");
    }

    @Test
    void texturedWorldGlyphAtlasResourceExists() {
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/font/terminal_hex_cp437.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/font/terminal_hex_cp437.png.mcmeta")));
    }
}
