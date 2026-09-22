package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class TerminalFontAtlasTest {
    @Test
    void everyAtlasGlyphMatchesTheOriginalBitmap() throws Exception {
        try (var chars = getClass().getResourceAsStream("/assets/neoopencomputers/textures/font/chars.txt");
             var texture = getClass().getResourceAsStream("/assets/neoopencomputers/textures/font/terminal_hex_cp437.png")) {
            assertNotNull(chars);
            assertNotNull(texture);
            final int[] codePoints = new String(chars.readAllBytes(), StandardCharsets.UTF_8).lines().findFirst().orElseThrow().codePoints().toArray();
            final var atlas = ImageIO.read(texture);
            assertEquals(128, atlas.getWidth());
            assertEquals(256, atlas.getHeight());
            for (int i = 0; i < Math.min(256, codePoints.length); i++) {
                final int cp = codePoints[i];
                if (!TerminalFont.hasGlyph(cp) || TerminalFont.glyphCellWidth(cp) != 8) continue;
                for (int y = 0; y < 16; y++) {
                    final int mask = TerminalFont.rowMask(cp, y);
                    for (int x = 0; x < 8; x++) {
                        assertEquals((mask & (1 << (7 - x))) != 0,
                            (atlas.getRGB(i % 16 * 8 + x, i / 16 * 16 + y) >>> 24) != 0,
                            "Code point " + cp + " at " + x + "," + y);
                    }
                }
            }
        }
    }
}
