package li.cil.oc.common.item;

import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.NativeVolumeStore;
import li.cil.oc.common.ModSettings;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class NativeDiskStorageGameTests {
    private static final byte[] BYTES = {97, 0, (byte) 128, (byte) 255, 42};

    @GameTest(template = "empty")
    public static void nativeManagedDisksStoreBytesInTheActiveWorld(GameTestHelper helper) throws Exception {
        final var world = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers");
        for (final var item : List.of(ModItems.HDD_TIER1.get(), ModItems.HDD_TIER2.get(), ModItems.HDD_TIER3.get(), ModItems.FLOPPY.get())) {
            final var stack = new ItemStack(item);
            final var environment = environment(stack);
            final var fs = (Component) environment.node();
            final var saved = new CompoundTag();
            try {
                final var writer = fs.invoke("open", null, "probe.bin", "wb")[0];
                fs.invoke("write", null, writer, BYTES); fs.invoke("close", null, writer);
                final var reader = fs.invoke("open", null, "probe.bin", "rb")[0];
                fs.invoke("read", null, reader, 1);
                environment.save(saved);
            } finally { environment.node().remove(); }
            final var metadata = saved.getCompound("fs");
            helper.assertTrue(!metadata.contains("children") && !metadata.getString(NativeVolumeStore.STORAGE_TAG).isEmpty(),
                "Native disk still embeds its file tree in item NBT instead of world storage");
            final var root = world.resolve(metadata.getString(NativeVolumeStore.STORAGE_TAG));
            helper.assertTrue(Arrays.equals(BYTES, Files.readAllBytes(root.resolve("probe.bin"))), "Native disk world bytes differ");
            final var restored = environment(stack);
            try {
                helper.assertTrue(saved.getCompound("node").getString("address").equals(restored.node().address()), "Disk component identity changed on remount");
                final int handle = metadata.getList("input", Tag.TAG_COMPOUND).getCompound(0).getInt("handle");
                helper.assertTrue(Arrays.equals(new byte[]{0, (byte) 128, (byte) 255, 42},
                    (byte[]) ((Component) restored.node()).invoke("read", null, handle, 16)[0]), "Disk read offset did not survive remount");
            } finally { restored.node().remove(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyLockedManagedDiskImportsWithoutChangingItsSource(GameTestHelper helper) throws Exception {
        final var stack = new ItemStack(ModItems.HDD_TIER1.get());
        final var source = ItemDriverData.dataTag(stack);
        final var inline = new CompoundTag();
        final var ram = FileSystem.fromMemory(65536);
        try {
            final int writer = ram.open("probe.bin", Mode.Write);
            ram.getHandle(writer).write(BYTES); ram.getHandle(writer).close();
            ram.setLastModified("probe.bin", 1000);
            ram.setLastModified("", 1000);
            ram.save(inline);
        } finally { ram.close(); }
        source.put("fs", inline);
        source.putString("oc:fs.label", "Legacy disk");
        source.putString("oc:lock", "Fixture owner");
        final var node = new CompoundTag(); node.putString("address", "legacy-native-disk-fixture"); source.put("node", node);
        final var before = source.copy();
        final var env = environment(stack);
        final var saved = new CompoundTag();
        try {
            helper.assertTrue(source.equals(before), "Native import mutated its source before a successful save");
            final var fs = (Component) env.node();
            helper.assertTrue("legacy-native-disk-fixture".equals(env.node().address()), "Import replaced a legacy component identity");
            helper.assertTrue(Boolean.TRUE.equals(fs.invoke("isReadOnly", null)[0]), "Import discarded the managed lock");
            helper.assertTrue("Legacy disk".equals(fs.invoke("getLabel", null)[0]), "Import discarded the disk label");
            final var reader = fs.invoke("open", null, "probe.bin", "rb")[0];
            helper.assertTrue(Arrays.equals(BYTES, (byte[]) fs.invoke("read", null, reader, 16)[0]), "Import lost binary content");
            env.save(saved);
        } finally { env.node().remove(); }
        helper.assertTrue(!saved.getCompound("fs").contains("children") && saved.getString("oc:lock").equals("Fixture owner"), "Imported locked disk did not persist its storage binding/owner");
        helper.assertTrue(saved.getCompound("fs").getString(NativeVolumeStore.STORAGE_TAG).startsWith("native-legacy/"), "Legacy ID was used as an unsafe storage path");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeDiskBufferChangesControlsActualDiskWrites(GameTestHelper helper) throws Exception {
        final boolean previous = ModSettings.BUFFER_CHANGES.get();
        final var world = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("opencomputers");
        try {
            for (final boolean buffered : List.of(false, true)) {
                ModSettings.BUFFER_CHANGES.set(buffered);
                for (final var item : List.of(ModItems.HDD_TIER1.get(), ModItems.HDD_TIER2.get(), ModItems.HDD_TIER3.get(), ModItems.FLOPPY.get())) {
                    final var stack = new ItemStack(item);
                    final var env = environment(stack);
                    try {
                        final var fs = (Component) env.node();
                        final long expected = (item instanceof HardDiskDriveItem hdd ? ModSettings.hddSize(hdd.tier(stack)) : ModSettings.floppySize()) * 1024L;
                        helper.assertTrue(((Number) fs.invoke("spaceTotal", null)[0]).longValue() == expected, "Native driver used the wrong configured capacity");
                        final var path = world.resolve(env.node().address()).resolve("probe.bin");
                        final var writer = fs.invoke("open", null, "probe.bin", "wb")[0];
                        fs.invoke("write", null, writer, BYTES); fs.invoke("close", null, writer);
                        helper.assertTrue(Files.exists(path) != buffered, "bufferChanges did not control write visibility on the physical filesystem");
                        if (!buffered) helper.assertTrue(Arrays.equals(BYTES, Files.readAllBytes(path)), "Direct native disk bytes differ before host save");
                        env.save(new CompoundTag());
                    } finally { env.node().remove(); }
                }
            }
        } finally { ModSettings.BUFFER_CHANGES.set(previous); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeDiskStorageBindingSurvivesAddressRemapping(GameTestHelper helper) throws Exception {
        final var stack = new ItemStack(ModItems.HDD_TIER1.get());
        final var env = environment(stack);
        final var original = new CompoundTag();
        try {
            final var fs = (Component) env.node();
            final var writer = fs.invoke("open", null, "probe.bin", "wb")[0];
            fs.invoke("write", null, writer, BYTES); fs.invoke("close", null, writer);
            env.save(original);
        } finally { env.node().remove(); }
        final var remapped = original.copy();
        remapped.getCompound("node").putString("address", "remapped-native-fixture");
        final var restored = HardDiskDriveItem.createEnvironment(remapped, saved -> {}, null);
        li.cil.oc.api.Network.joinNewNetwork(restored.node());
        try {
            final var fs = (Component) restored.node();
            ((Connector) fs).setLocalBufferSize(10000); ((Connector) fs).changeBuffer(10000);
            helper.assertTrue("remapped-native-fixture".equals(restored.node().address()), "Storage binding reverted a remapped component address");
            final var reader = fs.invoke("open", null, "probe.bin", "rb")[0];
            helper.assertTrue(Arrays.equals(BYTES, (byte[]) fs.invoke("read", null, reader, 16)[0]), "Address remapping detached the disk from its bytes");
            final var saved = new CompoundTag(); restored.save(saved);
            helper.assertTrue(original.getCompound("fs").getString(NativeVolumeStore.STORAGE_TAG).equals(saved.getCompound("fs").getString(NativeVolumeStore.STORAGE_TAG)), "Save replaced the independent storage binding");
            final var wrong = saved.copy(); wrong.getCompound("fs").putString(NativeVolumeStore.STORAGE_TAG, "00000000-0000-0000-0000-000000000000");
            boolean rejected = false;
            try { restored.load(wrong); } catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Live disk accepted an unrelated storage binding");
        } finally { restored.node().remove(); }
        helper.succeed();
    }

    private static ManagedEnvironment environment(ItemStack stack) {
        final var environment = Driver.driverFor(stack).createEnvironment(stack, null);
        li.cil.oc.api.Network.joinNewNetwork(environment.node());
        final var connector = (Connector) environment.node();
        connector.setLocalBufferSize(10000); connector.changeBuffer(10000);
        return environment;
    }
}
