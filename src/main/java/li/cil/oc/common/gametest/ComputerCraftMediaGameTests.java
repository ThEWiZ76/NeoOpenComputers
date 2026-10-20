package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class ComputerCraftMediaGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        return Loaded.tests();
    }
    public static ItemStack restartDisk() { return Loaded.disk(); }
    public static ItemStack hostRestartDisk() { return Loaded.hostDisk(); }
    public static void verifyHostRestartDisk(GameTestHelper helper, ItemStack stack) {
        Loaded.verifyNative(helper, stack, "host.bin", "CC host restart", new byte[]{'r', 0, (byte) 255, 'k'});
        li.cil.oc.NeoOpenComputers.LOGGER.info("CC internal/rack media bytes and label verified in PID {}", ProcessHandle.current().pid());
    }
    public static void verifyRestartDisk(GameTestHelper helper, ItemStack stack) {
        Loaded.verifyNative(helper, stack, "restart.bin", "process restart media", new byte[]{'a', 0, (byte) 255, 'z'});
        li.cil.oc.NeoOpenComputers.LOGGER.info("Native CC media restart bytes and label verified in PID {}", ProcessHandle.current().pid());
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(test("cc_media_native_disk", Loaded::nativeDisk), test("cc_media_readonly", Loaded::readOnly), test("cc_media_no_data", Loaded::noData), test("cc_media_deferred_world_load", Loaded::deferredWorldLoad), test("cc_media_case_internal_world_load", Loaded::internalCaseLoad));
        }
        static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> body) {
            return new TestFunction("ccMedia", name, "neoopencomputers:ae2_export", 600, 0, true, body);
        }
        static ItemStack disk() {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("computercraft", "disk")));
        }
        static ItemStack hostDisk() {
            final var stack = disk();
            if (!stack.getCapability(dan200.computercraft.api.media.MediaCapability.get()).setLabel(stack, "CC host restart")) throw new IllegalStateException("Native CC disk label rejected");
            return stack;
        }
        static void verifyNative(GameTestHelper helper, ItemStack stack, String path, String label, byte[] expected) {
            final var media = stack.getCapability(dan200.computercraft.api.media.MediaCapability.get());
            helper.assertTrue(media != null && label.equals(media.getLabel(helper.getLevel().registryAccess(), stack)), "CC native media label mismatch");
            try (var channel = media.createDataMount(stack, helper.getLevel()).openForRead(path)) {
                final var bytes = ByteBuffer.allocate(expected.length); while (bytes.hasRemaining() && channel.read(bytes) > 0) { }
                helper.assertTrue(bytes.position() == expected.length && java.util.Arrays.equals(bytes.array(), expected) && channel.size() == expected.length, "OC and CC do not see the same exact native disk bytes");
            } catch (Exception exception) { throw new RuntimeException(exception); }
        }
        static void nativeDisk(GameTestHelper helper) {
            final var hardware = DiskDriveEjectionGameTests.hardware(helper); final var stack = disk(); final var driver = Driver.driverFor(stack);
            helper.assertTrue(driver != null && hardware.drive().canPlaceItem(0, stack), "OC is missing the CC media item driver");
            helper.assertTrue(driver.slot(stack).equals(li.cil.oc.api.driver.item.Slot.Floppy) && driver.tier(stack) == 0, "CC media slot/tier differs from legacy");
            hardware.drive().setItem(0, stack);
            final var fs = DiskDriveEjectionGameTests.filesystem(helper, hardware); final String address = fs.address();
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            final var computer = hardware.computer();
            computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get())); computer.setItem(4, cpu);
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local drive = component.proxy(component.list('disk_drive')())
                local fs = component.proxy(assert(drive.media()))
                assert(not fs.isReadOnly())
                fs.setLabel('CC media Euro €')
                local h = assert(fs.open('oc.bin','wb'))
                assert(fs.write(h,string.char(0,128,255)..'Euro €')); fs.close(h)
                h = assert(fs.open('oc.bin','rb'))
                assert(fs.read(h,100) == string.char(0,128,255)..'Euro €'); fs.close(h)
                component.proxy(component.list('eeprom')()).setData('cc-media-verified')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); Network.joinOrCreateNetwork(computer); ((Connector) computer.node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "CC media native computer did not start");
            helper.startSequence().thenWaitUntil(() -> {
                helper.assertTrue(new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8).equals("cc-media-verified"), "Native CC media program failed: " + computer.machine().lastError());
            }).thenExecute(() -> {
                computer.machine().stop();
                final var text = "Euro €".getBytes(StandardCharsets.UTF_8); final byte[] expected = new byte[text.length + 3];
                expected[0] = 0; expected[1] = (byte) 128; expected[2] = (byte) 255; System.arraycopy(text, 0, expected, 3, text.length);
                final var removed = hardware.drive().removeItem(0, 1);
                final var decoded = ItemStack.parse(helper.getLevel().registryAccess(), removed.save(helper.getLevel().registryAccess())).orElseThrow();
                verifyNative(helper, decoded, "oc.bin", "CC media Euro €", expected);
                hardware.drive().setItem(0, decoded);
                final var restored = DiskDriveEjectionGameTests.filesystem(helper, hardware);
                helper.assertTrue(address.equals(restored.address()), "CC disk OC address changed after item serialization/reinsertion");
                hardware.drive().clearContent();
            }).thenSucceed();
        }
        static void readOnly(GameTestHelper helper) {
            final var hardware = DiskDriveEjectionGameTests.hardware(helper);
            final var stack = TestComputerCraftMediaCapabilities.resourceDisk();
            helper.assertTrue(Driver.driverFor(stack) != null, "Capability-only CC media was not detected");
            hardware.drive().setItem(0, stack); final var fs = DiskDriveEjectionGameTests.filesystem(helper, hardware); final var context = hardware.computer().machine();
            try {
                helper.assertTrue(Boolean.TRUE.equals(fs.invoke("isReadOnly", context)[0]) && fs.invoke("getLabel", context)[0].equals("CC ROM"), "Readonly media capability/label mismatch");
                final var entries = fs.invoke("list", context, "")[0];
                helper.assertTrue(entries instanceof Object[] values && values.length > 0, "Native CC ROM resource mount is empty");
                boolean rejected = false;
                try { fs.invoke("open", context, "forbidden", "wb"); } catch (Exception exception) { rejected = exception.getMessage().toLowerCase(java.util.Locale.ROOT).contains("read"); }
                helper.assertTrue(rejected, "Readonly media permitted writing");
                rejected = false;
                try { fs.invoke("setLabel", context, "changed"); } catch (Exception exception) { rejected = true; }
                helper.assertTrue(rejected && fs.invoke("getLabel", context)[0].equals("CC ROM"), "Immutable CC media label falsely reported success");
                hardware.drive().clearContent();
            } catch (Exception exception) { throw new RuntimeException(exception); }
            helper.succeed();
        }
        static void noData(GameTestHelper helper) {
            final var hardware = DiskDriveEjectionGameTests.hardware(helper);
            final var record = new ItemStack(Items.MUSIC_DISC_13); final var driver = Driver.driverFor(record);
            helper.assertTrue(record.getCapability(dan200.computercraft.api.media.MediaCapability.get()) != null && driver != null, "CC audio media capability was ignored");
            helper.assertTrue(driver.createEnvironment(record, hardware.drive()) == null && driver.createEnvironment(disk(), null) == null
                && Driver.driverFor(ItemStack.EMPTY) == null && Driver.driverFor(new ItemStack(Items.STONE)) == null, "No-data/null-host media created an invalid filesystem");
            hardware.drive().setItem(0, record);
            try { helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) hardware.drive().node()).invoke("isEmpty", hardware.computer().machine())[0]), "Audio-only media exposed a data filesystem"); }
            catch (Exception exception) { throw new RuntimeException(exception); }
            hardware.drive().clearContent(); helper.succeed();
        }

        static void deferredWorldLoad(GameTestHelper helper) {
            final var hardware = DiskDriveEjectionGameTests.hardware(helper);
            hardware.drive().setItem(0, disk());
            final String address = DiskDriveEjectionGameTests.filesystem(helper, hardware).address();
            final var saved = hardware.drive().saveWithFullMetadata(helper.getLevel().registryAccess());
            final var pos = hardware.drive().getBlockPos(); final var state = hardware.drive().getBlockState();
            helper.getLevel().removeBlockEntity(pos);
            final var loaded = new li.cil.oc.common.blockentity.DiskDriveBlockEntity(pos, state);
            loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
            loaded.setLevel(helper.getLevel()); helper.getLevel().setBlockEntity(loaded); loaded.onLoad();
            final var restored = DiskDriveEjectionGameTests.filesystem(helper, new DiskDriveEjectionGameTests.Hardware(loaded, hardware.computer()));
            helper.assertTrue(address.equals(restored.address()), "CC filesystem identity lost when block data loaded before world assignment");
            loaded.clearContent(); helper.succeed();
        }

        static void internalCaseLoad(GameTestHelper helper) {
            final var world = helper.getLevel(); final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3));
            final var state = li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER3.get().defaultBlockState();
            world.setBlockAndUpdate(pos, state);
            final var original = (li.cil.oc.common.blockentity.ComputerCaseBlockEntity) world.getBlockEntity(pos); original.onLoad();
            helper.assertTrue(original.canPlaceItem(7, disk()), "Tier3 internal floppy slot rejected native CC disk"); original.setItem(7, disk());
            final var fs = internalFilesystem(helper, original); final var address = fs.address();
            final var saved = original.saveWithFullMetadata(world.registryAccess()); world.removeBlockEntity(pos);
            final var loaded = new li.cil.oc.common.blockentity.ComputerCaseBlockEntity(pos, state);
            loaded.loadWithComponents(saved, world.registryAccess()); loaded.setLevel(world); world.setBlockEntity(loaded); loaded.onLoad();
            helper.assertTrue(address.equals(internalFilesystem(helper, loaded).address()), "Internal CC media identity lost on deferred case load");
            loaded.clearContent(); helper.succeed();
        }
        static li.cil.oc.api.network.Component internalFilesystem(GameTestHelper helper, li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer) {
            for (var node : computer.machine().node().reachableNodes()) if (node instanceof li.cil.oc.api.network.Component component && component.name().equals("filesystem")
                && computer.componentSlot(component.address()) == 7) return component;
            helper.fail("Tier3 case did not expose its internal CC filesystem"); return null;
        }
    }
}
