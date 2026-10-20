package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.LuaArchitecture;
import li.cil.oc.common.machine.NativeLua53Architecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

@GameTestHolder(NeoOpenComputers.MODID)
public final class ApiAddonGameTests {
    private static final BlockPos RESTART = new BlockPos(768, 80, 512);

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final var tests = new ArrayList<TestFunction>();
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return tests;
        for (boolean nativeVm : new boolean[]{false, true}) tests.add(new TestFunction("apiAddon", "external_api_addon_" + (nativeVm ? "native" : "luaj"),
            "neoopencomputers:empty", 400, 0, true, helper -> ordinary(helper, nativeVm)));
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!phase.isEmpty()) {
            if (!phase.equals("prepare") && !phase.equals("verify")) throw new IllegalArgumentException("Unknown API addon restart phase");
            NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
                final var level = event.getServer().overworld();
                if (level != null && level.hasChunkAt(RESTART) && level.getBlockEntity(RESTART) instanceof ComputerCaseBlockEntity computer)
                    ((Connector) computer.machine().node()).changeBuffer(10000);
            });
            tests.add(new TestFunction("apiAddonRestart", "external_api_addon_restart_" + phase, "neoopencomputers:empty", 600, 0, true,
                helper -> restart(helper, phase.equals("prepare"))));
        }
        return tests;
    }

    private static void assertAddon(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("oc_api_smoke"), "Explicit API addon profile did not load its standalone mod");
        helper.assertTrue(Driver.driverFor(card()) != null && Driver.driverFor(card()).getClass().getName().startsWith("example.ocapi."),
            "Standalone addon driver was not registered");
        boolean rejected = false;
        try { Driver.add((li.cil.oc.api.driver.Converter) (value, output) -> { }); }
        catch (IllegalStateException expected) { rejected = true; }
        helper.assertTrue(rejected, "Driver registration must still close after mod loading");
    }

    private static void ordinary(GameTestHelper helper, boolean nativeVm) {
        assertAddon(helper);
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        final ComputerCaseBlockEntity[] computer = {install(helper, pos, nativeVm, nativeVm)};
        final var sequence = helper.startSequence();
        if (nativeVm) sequence.thenWaitUntil(() -> helper.assertTrue(marker(computer[0]).equals("prepared"), "Addon did not reach reload checkpoint"))
            .thenExecute(() -> {
                final var original = computer[0]; original.onChunkUnloaded();
                final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess()); original.setRemoved();
                computer[0] = (ComputerCaseBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, original.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(computer[0]); computer[0].onLoad();
                helper.assertTrue(computer[0].machine().signal("api_addon_resume"), "Reloaded addon signal rejected");
            });
        sequence.thenWaitUntil(() -> helper.assertTrue(marker(computer[0]).equals("verified"),
            "External addon callback/converter/value failed: " + computer[0].machine().lastError())).thenSucceed();
    }

    private static void restart(GameTestHelper helper, boolean prepare) {
        assertAddon(helper);
        final var level = helper.getLevel();
        level.setChunkForced(RESTART.getX() >> 4, RESTART.getZ() >> 4, true);
        helper.runAfterDelay(3, () -> {
            if (prepare) {
                helper.assertTrue(level.isEmptyBlock(RESTART) && level.isEmptyBlock(RESTART.south(2)), "API addon checkpoint already exists");
                level.setBlockAndUpdate(RESTART.south(2), Blocks.CHEST.defaultBlockState());
                install(helper, RESTART, true, true);
            }
            helper.assertTrue(level.getBlockEntity(RESTART) instanceof ComputerCaseBlockEntity && level.getBlockEntity(RESTART.south(2)) instanceof ChestBlockEntity,
                "Saved addon hosts/checkpoint missing");
            final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(RESTART);
            final var record = (ChestBlockEntity) level.getBlockEntity(RESTART.south(2));
            if (!prepare) {
                final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA);
                helper.assertTrue(data != null && data.copyTag().getString("phase").equals("prepared") && data.copyTag().getLong("pid") > 0
                    && data.copyTag().getLong("pid") != ProcessHandle.current().pid(), "Addon checkpoint must come from another JVM");
                helper.assertTrue(data.copyTag().getString("machine").equals(computer.machine().node().address()) && marker(computer).equals("prepared")
                    && computer.machine().isRunning(), "Addon machine identity/continuation lost: " + computer.machine().lastError());
                helper.assertTrue(computer.machine().signal("api_addon_resume"), "Addon resume signal rejected");
            }
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals(prepare ? "prepared" : "verified"),
                    "Addon persistence failed: " + computer.machine().lastError()))
                .thenExecute(() -> {
                    final var data = new CompoundTag(); data.putString("phase", prepare ? "prepared" : "verified");
                    data.putString("machine", computer.machine().node().address()); data.putLong("pid", ProcessHandle.current().pid());
                    final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); record.setItem(0, paper); record.setChanged();
                    NeoOpenComputers.LOGGER.info("External API addon {} in PID {}", prepare ? "prepared" : "verified", ProcessHandle.current().pid());
                }).thenSucceed();
        });
    }

    private static ComputerCaseBlockEntity install(GameTestHelper helper, BlockPos pos, boolean nativeVm, boolean persist) {
        final var level = helper.getLevel(); level.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, nativeVm ? NativeLua53Architecture.class : LuaArchitecture.class);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu); computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, card());
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom=component.proxy(component.list('eeprom')())
            assert(eeprom.getData()=='', 'addon machine rebooted')
            local card=component.proxy(assert(component.list('api_smoke')())); local address=card.address
            assert(card.increment(731)==731 and card.getCount()==731, 'addon callbacks failed')
            local description=card.describe('external'); assert(description.name=='external' and description.bytes==string.char(0,255,42), 'addon converter failed')
            local counter=card.counter(37); local alias=counter; assert(counter.add(5)==42, 'addon value failed')
            %s
            assert(alias==counter and counter.add(1)==43, 'addon userdata alias/state lost')
            assert(card.address==address and card.getCount()==731 and card.increment(1)==732, 'addon environment state lost')
            eeprom.setData('verified')
            while true do computer.pullSignal() end
            """.formatted(persist ? "eeprom.setData('prepared'); repeat until computer.pullSignal()=='api_addon_resume'" : "")));
        ((Connector) computer.machine().node()).changeBuffer(10000);
        helper.assertTrue(computer.toggleMachine(), "Addon fixture did not start");
        return computer;
    }

    private static ItemStack card() {
        final var stack = new ItemStack(Items.PAPER); final var data = new CompoundTag(); data.putBoolean("oc_api_smoke", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(ComputerCaseBlockEntity.SLOT_EEPROM)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
