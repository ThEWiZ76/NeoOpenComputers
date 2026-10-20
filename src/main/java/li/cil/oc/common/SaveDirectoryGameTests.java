package li.cil.oc.common;

import li.cil.oc.api.fs.Mode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class SaveDirectoryGameTests {
    @GameTest(template = "empty")
    public static void bufferedSaveDirectoryHonorsForcedCasePolicy(GameTestHelper helper) throws Exception {
        final var world = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT);
        final var parent = world.resolve("opencomputers");
        Files.createDirectories(parent);
        final var directory = Files.createTempDirectory(parent, "oc-case-fixture-");
        final boolean previous = ModSettings.FORCE_CASE_INSENSITIVE_FS.get();
        li.cil.oc.api.fs.FileSystem fs = null;
        try {
            ModSettings.FORCE_CASE_INSENSITIVE_FS.set(true);
            fs = li.cil.oc.api.FileSystem.fromSaveDirectory(directory.getFileName().toString(), 8192, true);
            helper.assertTrue(fs != null, "Buffered public API did not mount the active world");
            final int writer = fs.open("Boot.lua", Mode.Write);
            fs.getHandle(writer).write(new byte[]{97, 0, (byte) 255});
            fs.getHandle(writer).close();
            helper.assertTrue(fs.exists("BOOT.LUA"), "Configured buffered filesystem is case-sensitive");
            final var metadata = new net.minecraft.nbt.CompoundTag();
            fs.save(metadata);
            fs.close();
            fs = li.cil.oc.api.FileSystem.fromSaveDirectory(directory.getFileName().toString(), 8192, true);
            helper.assertTrue(fs != null && fs.exists("boot.LUA"), "Saved case policy was lost on remount");
            final var ram = li.cil.oc.api.FileSystem.fromMemory(8192);
            try {
                ram.getHandle(ram.open("Boot.lua", Mode.Write)).close();
                helper.assertTrue(!ram.exists("BOOT.LUA"), "Buffered config changed RAM filesystem semantics");
            } finally { ram.close(); }
        } finally {
            ModSettings.FORCE_CASE_INSENSITIVE_FS.set(previous);
            if (fs != null) fs.close();
            Files.deleteIfExists(directory.resolve("Boot.lua"));
            Files.deleteIfExists(directory);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void addonSaveDirectoryUsesTheActiveWorld(GameTestHelper helper) throws Exception {
        final String name = "oc-api-world-root-fixture";
        final Path processRoot = Path.of(name).toAbsolutePath().normalize();
        final Path worldRoot = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        final Path expected = worldRoot.resolve("opencomputers").resolve(name);
        helper.assertTrue(!Files.exists(processRoot), "Preserve an existing process-relative fixture directory");
        final var fs = new FileSystemRegistry().fromSaveDirectory(name, 4096, false);
        try {
            helper.assertTrue(fs != null, "Active server did not supply an addon filesystem");
            final int id = fs.open("world-marker.bin", Mode.Write);
            fs.getHandle(id).write("world1".getBytes(StandardCharsets.UTF_8));
            fs.getHandle(id).close();
            helper.assertTrue(Files.isRegularFile(expected.resolve("world-marker.bin")) && !Files.exists(processRoot),
                "Addon filesystem writes into the process directory instead of the active world's opencomputers folder");
            helper.assertTrue("world1".equals(Files.readString(expected.resolve("world-marker.bin"))), "World-local addon file contents differ");
        } finally {
            if (fs != null) fs.close();
            // These exact fixture paths were absent before the test; no recursive cleanup.
            Files.deleteIfExists(processRoot.resolve("world-marker.bin"));
            Files.deleteIfExists(processRoot);
            Files.deleteIfExists(expected.resolve("world-marker.bin"));
            Files.deleteIfExists(expected);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void addonSaveDirectoryRejectsAnAbsoluteHostPath(GameTestHelper helper) throws Exception {
        final Path worldRoot = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        final Path absolute = worldRoot.resolve("oc-api-absolute-root-fixture");
        helper.assertTrue(!Files.exists(absolute), "Preserve an existing absolute-path fixture directory");
        li.cil.oc.api.fs.FileSystem fs = null;
        boolean rejected = false;
        try {
            fs = new FileSystemRegistry().fromSaveDirectory(absolute.toString(), 4096, false);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        } finally {
            if (fs != null) fs.close();
            Files.deleteIfExists(absolute);
        }
        helper.assertTrue(rejected, "Save-directory API accepted an absolute host path instead of a world-relative name");
        helper.succeed();
    }
}
