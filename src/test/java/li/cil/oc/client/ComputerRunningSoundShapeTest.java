package li.cil.oc.client;

import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerRunningSoundShapeTest {
    @Test
    void computerCaseSyncsRunningStateToClient() throws NoSuchMethodException {
        assertEquals(ComputerCaseBlockEntity.class, ComputerCaseBlockEntity.class.getDeclaredMethod("getUpdatePacket").getDeclaringClass());
        assertEquals(ComputerCaseBlockEntity.class, ComputerCaseBlockEntity.class.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class).getDeclaringClass());
        assertEquals(ComputerCaseBlockEntity.class, ComputerCaseBlockEntity.class.getDeclaredMethod("handleUpdateTag", CompoundTag.class, HolderLookup.Provider.class).getDeclaringClass());
        assertEquals(boolean.class, ComputerCaseBlockEntity.class.getDeclaredMethod("isClientRunning").getReturnType());
    }

    @Test
    void clientMaintainsLoopingComputerRunningSound() throws Exception {
        final Path sourcePath = Path.of("src/main/java/li/cil/oc/client/ComputerCaseSounds.java");

        assertTrue(Files.exists(sourcePath));

        final String source = Files.readString(sourcePath);
        assertTrue(source.contains("ModSounds.COMPUTER_RUNNING_ID"));
        assertTrue(source.contains("AbstractTickableSoundInstance"));
        assertTrue(source.contains("looping = true"));
        assertTrue(source.contains("getSoundManager().play"));
        assertTrue(source.contains("isClientRunning()"));
    }

    @Test
    void clientEntrypointImportsComputerCaseSounds() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("ComputerCaseSounds"));
    }
}
