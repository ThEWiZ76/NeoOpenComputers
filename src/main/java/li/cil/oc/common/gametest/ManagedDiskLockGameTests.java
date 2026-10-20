package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import li.cil.oc.common.item.HardDiskDriveItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ManagedDiskLockGameTests {
    private static final byte[] BYTES = {97, 0, (byte) 128, (byte) 255, 42};

    @GameTest(template = "empty")
    public static void managedHddLockRejectsMutationsAndPreservesBytes(GameTestHelper helper) throws Exception {
        for (final var item : List.of(ModItems.HDD_TIER1.get(), ModItems.HDD_TIER2.get(), ModItems.HDD_TIER3.get())) {
            final var stack = prepared(new ItemStack(item));
            HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
            final var environment = environment(stack);
            final var fs = (Component) environment.node();
            try {
                helper.assertTrue(Boolean.TRUE.equals(fs.invoke("isReadOnly", null)[0]), "Locked managed HDD still reports writable");
                denied(helper, () -> fs.invoke("open", null, "probe.bin", "wb"));
                denied(helper, () -> fs.invoke("open", null, "probe.bin", "ab"));
                helper.assertTrue(Boolean.FALSE.equals(fs.invoke("remove", null, "probe.bin")[0]) && Boolean.FALSE.equals(fs.invoke("rename", null, "probe.bin", "changed.bin")[0])
                    && Boolean.FALSE.equals(fs.invoke("makeDirectory", null, "changed")[0]), "Locked managed HDD accepted filesystem mutation");
                final var handle = fs.invoke("open", null, "probe.bin", "rb")[0];
                helper.assertTrue(Arrays.equals(BYTES, (byte[]) fs.invoke("read", null, handle, 16)[0]), "Locking destroyed existing binary content");
                fs.invoke("close", null, handle);
            } finally { environment.node().remove(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void managedHddLockProtectsTheExistingLabel(GameTestHelper helper) throws Exception {
        final var stack = prepared(new ItemStack(ModItems.HDD_TIER1.get()));
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        final var environment = environment(stack);
        final var fs = (Component) environment.node();
        try {
            denied(helper, () -> fs.invoke("setLabel", null, "Changed"));
            helper.assertTrue("Keep me".equals(fs.invoke("getLabel", null)[0]), "Locked HDD label changed or was not loaded");
        } finally { environment.node().remove(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void managedHddLockSurvivesComponentAndItemSerialization(GameTestHelper helper) throws Exception {
        final var stack = prepared(new ItemStack(ModItems.HDD_TIER1.get()));
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        final var environment = environment(stack);
        final var snapshot = new CompoundTag();
        try { environment.save(snapshot); } finally { environment.node().remove(); }
        helper.assertTrue(snapshot.getString("oc:lock").equals("Fixture owner") && HardDiskDriveItem.lockInfo(stack).equals("Fixture owner"), "Saving a managed HDD discarded its lock metadata");
        final var restoredStack = ItemStack.parse(helper.getLevel().registryAccess(), stack.save(helper.getLevel().registryAccess())).orElseThrow();
        final var restored = environment(restoredStack);
        try {
            helper.assertTrue(HardDiskDriveItem.lockInfo(restoredStack).equals("Fixture owner") && Boolean.TRUE.equals(((Component) restored.node()).invoke("isReadOnly", null)[0]), "Serialized locked managed HDD reopened writable");
        } finally { restored.node().remove(); }
        helper.succeed();
    }

    static ItemStack prepared(ItemStack stack) throws Exception {
        final var environment = environment(stack);
        final var fs = (Component) environment.node();
        try {
            final var handle = fs.invoke("open", null, "probe.bin", "wb")[0];
            fs.invoke("write", null, handle, BYTES); fs.invoke("close", null, handle);
            fs.invoke("setLabel", null, "Keep me");
            environment.save(new CompoundTag());
        } finally { environment.node().remove(); }
        return stack;
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nativeComputerCannotWriteItsLockedManagedHdd(GameTestHelper helper) throws Exception {
        final var stack = prepared(new ItemStack(ModItems.HDD_TIER1.get()));
        HardDiskDriveItem.lock(HardDiskDriveItem.stackDataTag(stack), "Fixture owner");
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.COMPUTER_CASE_TIER1.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 1));
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu);
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, stack);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom=component.proxy(component.list('eeprom')())
            local disk
            for address in component.list('filesystem') do
              local fs=component.proxy(address)
              if fs.getLabel()=='Keep me' then disk=fs end
            end
            assert(disk and disk.isReadOnly(),'Installed locked HDD is writable or missing')
            for _,mode in ipairs({'wb','ab'}) do
              local ok,h=pcall(disk.open,'probe.bin',mode)
              assert(not ok or not h,'Locked HDD accepted write-open')
            end
            assert(not disk.remove('probe.bin') and not disk.rename('probe.bin','changed.bin')
              and not disk.makeDirectory('changed'),'Locked HDD accepted mutation')
            local ok,label=pcall(disk.setLabel,'Changed')
            assert(not ok or not label,'Locked HDD label changed')
            local h=assert(disk.open('probe.bin','rb'))
            assert(disk.read(h,16)==string.char(97,0,128,255,42),'Locked HDD binary data lost')
            disk.close(h)
            assert(disk.getLabel()=='Keep me','Locked HDD label lost')
            eeprom.setData('locked-hdd-ok')
            while true do computer.pullSignal() end
            """));
        computer.onLoad(); ((Connector) computer.machine().node()).changeBuffer(10000);
        helper.assertTrue(computer.machine().start(), "Locked HDD native test did not start");
        helper.startSequence().thenWaitUntil(() -> {
            final var data = ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM));
            helper.assertTrue(new String(data.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("locked-hdd-ok"), "Locked HDD native program failed: " + computer.machine().lastError());
        }).thenExecute(() -> {
            computer.saveWithFullMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(HardDiskDriveItem.lockInfo(computer.getItem(ComputerCaseBlockEntity.SLOT_HDD)).equals("Fixture owner"), "Actual host save lost the HDD lock");
        }).thenSucceed();
    }

    private static ManagedEnvironment environment(ItemStack stack) {
        final var environment = Driver.driverFor(stack).createEnvironment(stack, null);
        final var connector = (Connector) environment.node();
        connector.setLocalBufferSize(10000); connector.changeBuffer(10000);
        return environment;
    }

    private static void denied(GameTestHelper helper, Attempt action) throws Exception {
        boolean denied = false;
        try { action.run(); } catch (java.io.IOException | IllegalArgumentException failure) {
            helper.assertTrue(failure.getMessage().contains("read only") || failure.getMessage().contains("read-only"), "Mutation failed for another reason: " + failure);
            denied = true;
        }
        helper.assertTrue(denied, "Locked managed HDD accepted a mutation");
    }

    @FunctionalInterface private interface Attempt { void run() throws Exception; }
}
