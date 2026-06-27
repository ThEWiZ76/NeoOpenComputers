package li.cil.oc.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoundResourceTest {
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/neoopencomputers");
    private static final Path SOUND_CATALOG = ASSET_ROOT.resolve("sounds.json");
    private static final List<String> SOUND_KEYS = List.of(
        "computer_running",
        "floppy_access",
        "floppy_eject",
        "floppy_insert",
        "hdd_access"
    );

    @Test
    void upstreamSoundCatalogIsRestoredUnderNeoNamespace() throws IOException {
        assertTrue(Files.exists(SOUND_CATALOG), "Missing sounds.json");
        try (Reader reader = Files.newBufferedReader(SOUND_CATALOG)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (final String key : SOUND_KEYS) {
                assertTrue(root.has(key), "Missing sound key " + key);
                final JsonArray sounds = root.getAsJsonObject(key).getAsJsonArray("sounds");
                assertFalse(sounds.isEmpty(), "No sound variants for " + key);
                for (final JsonElement element : sounds) {
                    final String name = element.isJsonObject()
                        ? element.getAsJsonObject().get("name").getAsString()
                        : element.getAsString();
                    assertTrue(name.startsWith("neoopencomputers:"), "Wrong namespace for " + key + ": " + name);
                    assertFalse(name.startsWith("opencomputers:"), "Legacy namespace kept for " + key + ": " + name);
                    final String path = name.substring("neoopencomputers:".length());
                    assertTrue(Files.exists(ASSET_ROOT.resolve("sounds").resolve(path + ".ogg")), "Missing OGG for " + name);
                }
            }
        }
    }

    @Test
    void storageDriversUseUpstreamAccessSoundIds() throws IOException {
        final String floppy = Files.readString(Path.of("src/main/java/li/cil/oc/common/item/FloppyItem.java"));
        final String hdd = Files.readString(Path.of("src/main/java/li/cil/oc/common/item/HardDiskDriveItem.java"));

        assertTrue(floppy.contains("ModSounds.FLOPPY_ACCESS_ID"), "Floppy file systems should emit floppy access sound");
        assertTrue(hdd.contains("ModSounds.HDD_ACCESS_ID"), "Hard disks should emit hdd access sound");
    }

    @Test
    void filesystemAccessHandlerPlaysAccessSoundLikeUpstream() throws IOException {
        final String handler = Files.readString(Path.of("src/main/java/li/cil/oc/common/component/FileSystemAccessHandler.java"));
        final String diskDrive = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/DiskDriveBlockEntity.java"));

        assertTrue(handler.contains("ModSounds.soundEvent"), "Handler should resolve custom sound events");
        assertTrue(handler.contains("ModSounds.play(event.getWorld()"), "Handler should play filesystem access sounds on server");
        assertTrue(handler.contains("DiskDriveBlockEntity"), "Handler should update disk-drive activity timestamps");
        assertTrue(handler.contains("recordDiskDriveAccess"), "Handler should resolve block disk drives for access activity");
        assertTrue(diskDrive.contains("recordFileSystemAccess"), "Disk drive should expose upstream last-access tracking");
        assertTrue(diskDrive.contains("TAG_LAST_ACCESS"), "Disk drive should sync last access to the client renderer");
    }

    @Test
    void diskDriveInsertAndEjectUseUpstreamSounds() throws IOException {
        final String block = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/DiskDriveBlock.java"));
        final String blockEntity = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/DiskDriveBlockEntity.java"));

        assertFalse(block.contains("ModSounds.playDiskInsert"), "Block use path should not double-play insert sounds");
        assertFalse(block.contains("ModSounds.playDiskEject"), "Block use path should not double-play eject sounds");
        assertTrue(blockEntity.contains("ModSounds.playDiskInsert"), "Disk-drive inventory inserts should play upstream insert sound");
        assertTrue(blockEntity.contains("ModSounds.playDiskEject"), "Disk-drive inventory removals should play upstream eject sound");
    }
}
