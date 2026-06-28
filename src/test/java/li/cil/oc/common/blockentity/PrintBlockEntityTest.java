package li.cil.oc.common.blockentity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrintBlockEntityTest {
    @Test
    void playsUpstreamLeverClickSoundWhenTogglingActiveState() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/PrintBlockEntity.java"));

        assertTrue(source.contains("SoundEvents.LEVER_CLICK"));
        assertTrue(source.contains("SoundSource.BLOCKS"));
        assertTrue(source.contains("level.playSound(null, worldPosition"));
        assertTrue(source.contains("0.3F"));
        assertEquals(0.6F, PrintBlockEntity.toggleStatePitch(true));
        assertEquals(0.5F, PrintBlockEntity.toggleStatePitch(false));
    }
}
