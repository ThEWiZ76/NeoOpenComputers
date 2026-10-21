package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Actual chunk/entity saves between distinct JVMs, without reconstructing host NBT in verify. */
@GameTestHolder(NeoOpenComputers.MODID)
public final class NativeHostsProcessRestartGameTests {
    private record Fixture(String kind, String version, int index) {
        BlockPos pos() { return new BlockPos(512 + index * 16, 80, 512); }
        UUID droneId() { return new UUID(0x4f435f4e41544956L, 100 + index); }
    }
    private record Host(Machine machine, Iterable<ItemStack> hardware) { }
    private static List<Fixture> fixtures() {
        final var result = new ArrayList<Fixture>();
        for (final var version : List.of("5.3", "5.4"))
            for (final var kind : List.of("case", "rack", "microcontroller", "robot", "drone"))
                result.add(new Fixture(kind, version, result.size()));
        if (Boolean.getBoolean("neoopencomputers.testApiSmokeAddon"))
            for (final var version : List.of("5.3", "5.4"))
                for (final var kind : List.of("charger", "block_adapter", "block_adapter_nodeless"))
                    result.add(new Fixture(kind, version, result.size()));
        return result;
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.restartNativeHosts")) return List.of();
        final String phase = System.getProperty("neoopencomputers.restartPhase", "");
        if (!List.of("prepare", "verify").contains(phase)) throw new IllegalStateException("Native hosts require a restart phase");
        final var fixtures = fixtures();
        // Saved forced chunks execute before the test batch begins; supply bounded fixture power immediately.
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            final var level = event.getServer().overworld();
            if (level == null) return;
            for (final var fixture : fixtures) if (level.hasChunkAt(fixture.pos())) {
                final var host = loadedHost(level, fixture);
                if (host != null) ((Connector) host.machine.node()).changeBuffer(10000);
            }
        });
        return fixtures.stream().map(fixture -> new TestFunction("nativeHostsRestart",
            "native_" + fixture.kind + "_" + fixture.version.replace('.', '_') + "_restart_" + phase,
            "neoopencomputers:empty", 600, 0, true, helper -> run(helper, fixture, phase.equals("prepare")))).toList();
    }

    private static Host loadedHost(ServerLevel level, Fixture fixture) {
        if (fixture.kind.equals("drone")) {
            if (level.getEntity(fixture.droneId()) instanceof DroneEntity drone) return new Host(drone.machine(), drone.internalComponents());
            return null;
        }
        final var entity = level.getBlockEntity(fixture.pos());
        if (entity instanceof ComputerCaseBlockEntity computer) return new Host(computer.machine(), stacks(computer));
        if (entity instanceof MicrocontrollerBlockEntity micro) return new Host(micro.machine(), stacks(micro));
        if (entity instanceof RobotBlockEntity robot) return new Host(robot.machine(), robot.internalComponents());
        if (entity instanceof RackBlockEntity rack && rack.getMountable(0) instanceof li.cil.oc.api.internal.Server server)
            return new Host(server.machine(), stacks((Container) server));
        return null;
    }

    private static List<ItemStack> stacks(Container container) {
        final var result = new ArrayList<ItemStack>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) result.add(container.getItem(slot));
        return result;
    }

    private static void run(GameTestHelper helper, Fixture fixture, boolean prepare) {
        final var level = helper.getLevel(); final var pos = fixture.pos();
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        // Rack mountables and saved entities become available after the chunk's onLoad phase.
        helper.runAfterDelay(5, () -> {
            if (prepare) install(helper, fixture);
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(loadedHost(level, fixture) != null, "Saved host missing: " + fixture))
                .thenExecute(() -> continueProbe(helper, fixture, prepare));
        });
    }

    private static void install(GameTestHelper helper, Fixture fixture) {
        final var level = helper.getLevel(); final var pos = fixture.pos();
        helper.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.south(2)) && level.getEntity(fixture.droneId()) == null,
            "Native host probe requires a fresh world");
        level.setBlockAndUpdate(pos.south(2), Blocks.CHEST.defaultBlockState());
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final var architecture = li.cil.oc.api.Machine.architectures().stream()
            .filter(type -> li.cil.oc.api.Machine.getArchitectureName(type).equals("Lua " + fixture.version)).findFirst().orElseThrow();
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, architecture);
        final var ram = new ItemStack(ModItems.MEMORY_TIER1.get());
        final var eeprom = RobotMovementPersistenceGameTests.eeprom(program(fixture));
        final boolean addon = Boolean.getBoolean("neoopencomputers.testApiSmokeAddon");
        final var card = addon ? AddonLifecycleGameTests.card("native-" + fixture.kind + "-" + fixture.version, false) : ItemStack.EMPTY;
        switch (fixture.kind) {
            case "case", "charger", "block_adapter", "block_adapter_nodeless" -> {
                level.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
                final var host = (ComputerCaseBlockEntity) level.getBlockEntity(pos);
                host.setItem(ComputerCaseBlockEntity.SLOT_CPU, cpu); host.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, ram);
                host.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, eeprom);
                if (fixture.kind.equals("charger")) {
                    helper.assertTrue(level.isEmptyBlock(pos.east()), "Charger restart fixture requires empty companion space");
                    level.setBlockAndUpdate(pos.east(), ModBlocks.CHARGER.get().defaultBlockState());
                    final var charger = (ChargerBlockEntity) level.getBlockEntity(pos.east());
                    final var data = card.get(DataComponents.CUSTOM_DATA).copyTag();
                    data.putBoolean("oc_api_smoke_tablet", true); data.putBoolean("oc_api_smoke_require_world", true);
                    card.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                    helper.assertTrue(charger.canPlaceItem(0, card), "Charger restart addon item rejected");
                    charger.setItem(0, card);
                } else if (fixture.kind.startsWith("block_adapter")) {
                    helper.assertTrue(level.isEmptyBlock(pos.east()) && level.isEmptyBlock(pos.east(2)), "Block adapter restart fixture needs empty companion space");
                    level.setBlockAndUpdate(pos.east(2), Blocks.BARREL.defaultBlockState());
                    AddonBlockLifecycleGameTests.name(helper, (BarrelBlockEntity) level.getBlockEntity(pos.east(2)), fixture.kind.endsWith("nodeless"), 1);
                    level.setBlockAndUpdate(pos.east(), ModBlocks.ADAPTER.get().defaultBlockState());
                    final var adapter = (AdapterBlockEntity) level.getBlockEntity(pos.east());
                    adapter.onLoad(); adapter.refreshNeighbors();
                    if (fixture.kind.endsWith("nodeless")) {
                        final var data = card.get(DataComponents.CUSTOM_DATA).copyTag();
                        data.putString("oc_api_smoke_block_control", AddonBlockLifecycleGameTests.blockId(pos.east(2), Direction.EAST));
                        card.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                        host.setItem(0, card);
                    }
                } else if (addon) host.setItem(0, card);
            }
            case "rack" -> {
                level.setBlockAndUpdate(pos, ModBlocks.RACK.get().defaultBlockState());
                final var rack = (RackBlockEntity) level.getBlockEntity(pos); rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
                final var items = (Container) rack.getMountable(0); items.setItem(2, cpu); items.setItem(4, ram); items.setItem(8, eeprom);
                if (addon) items.setItem(0, card);
            }
            case "microcontroller" -> {
                level.setBlockAndUpdate(pos, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState());
                final var host = (MicrocontrollerBlockEntity) level.getBlockEntity(pos);
                MicrocontrollerFixture.setItem(host, 0, cpu); MicrocontrollerFixture.setItem(host, 1, ram); MicrocontrollerFixture.setItem(host, 2, eeprom);
                if (addon) {
                    helper.assertTrue(MicrocontrollerFixture.canPlaceItem(host, 3, card), "Microcontroller addon slot rejected");
                    MicrocontrollerFixture.setItem(host, 3, card);
                }
                host.onLoad();
            }
            case "robot" -> {
                level.setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
                final var host = (RobotBlockEntity) level.getBlockEntity(pos);
                RobotMovementPersistenceGameTests.installHardware(helper, host, addon ? List.of(cpu, ram, eeprom, card) : List.of(cpu, ram, eeprom)); host.onLoad();
            }
            case "drone" -> {
                final var host = new DroneEntity(level); host.setUUID(fixture.droneId()); host.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
                host.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(new ItemStack(ModItems.DRONE_CASE_TIER1.get()), addon ? new ItemStack[]{cpu, ram, eeprom, card} : new ItemStack[]{cpu, ram, eeprom}), null);
                helper.assertTrue(level.addFreshEntity(host), "Drone spawn failed");
            }
            default -> throw new IllegalArgumentException(fixture.kind);
        }
    }

    private static void continueProbe(GameTestHelper helper, Fixture fixture, boolean prepare) {
        final var host = loadedHost(helper.getLevel(), fixture);
        final var record = (Container) helper.getLevel().getBlockEntity(fixture.pos().south(2));
        helper.assertTrue(record != null, "Saved host record missing");
        if (prepare) {
            ((Connector) host.machine.node()).changeBuffer(10000);
            helper.assertTrue(host.machine.start(), "Native host did not start");
        } else {
            final var data = record.getItem(0).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(data != null, "Saved checkpoint missing");
            final var saved = data.copyTag();
            helper.assertTrue(saved.getString("phase").equals("prepared") && saved.getLong("pid") > 0 && saved.getLong("pid") != ProcessHandle.current().pid(),
                "Checkpoint not prepared by a distinct JVM");
            helper.assertTrue(saved.getString("machine").equals(host.machine.node().address()), "Saved machine address changed");
            helper.assertTrue(marker(host).equals("prepared") && host.machine.isRunning(), "Saved VM did not remain running: " + host.machine.lastError());
            helper.assertTrue(host.machine.signal("resume_native_host"), "Saved VM rejected signal");
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(host).equals(prepare ? "prepared" : "verified"),
                "Native continuation failed for " + fixture + ": " + host.machine.lastError()))
            .thenExecute(() -> {
                final var tag = new CompoundTag(); tag.putString("phase", prepare ? "prepared" : "verified");
                tag.putLong("pid", ProcessHandle.current().pid()); tag.putString("machine", host.machine.node().address());
                final var paper = new ItemStack(Items.PAPER); paper.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); record.setItem(0, paper); record.setChanged();
                NeoOpenComputers.LOGGER.info("Native {} Lua {} {} in PID {}", fixture.kind, fixture.version, prepare ? "prepared" : "verified", ProcessHandle.current().pid());
            }).thenSucceed();
    }

    private static String marker(Host host) {
        for (final var stack : host.hardware) if (stack.is(ModItems.EEPROM.get()))
            return new String(ItemDriverData.dataTag(stack).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
        throw new IllegalStateException("Host EEPROM missing");
    }

    private static String program(Fixture fixture) {
        final String version = fixture.version;
        final String addonName = fixture.kind.equals("block_adapter_nodeless") ? "api_block_control"
            : fixture.kind.equals("block_adapter") ? "api_block_lifecycle" : "api_lifecycle";
        return """
            assert(_VERSION == 'Lua %s' and computer.getArchitecture() == 'Lua %s', 'wrong native VM')
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local addon
            if %s then
              addon=component.proxy(assert(component.list('%s')(), 'addon missing'))
              assert(addon.getPhase()=='Initialized' and addon.setValue(731)==731, 'addon not initialized')
            end
            local retained = {value=731}; retained.self=retained
            local fs = component.proxy(computer.tmpAddress()); local address=fs.address
            local file=assert(fs.open('native-host.bin','wb')); assert(fs.write(file,'a'..string.char(0,255)..'z')); fs.close(file)
            file=assert(fs.open('native-host.bin','rb')); assert(fs.read(file,1)=='a'); local alias=file
            eeprom.setData('prepared')
            repeat until computer.pullSignal() == 'resume_native_host'
            assert(retained.value==731 and retained.self==retained, 'Lua local/cycle lost')
            if addon then assert(addon.getPhase()=='Initialized' and addon.getValue()==731, 'addon lifecycle/state lost') end
            assert(_VERSION=='Lua %s' and computer.getArchitecture()=='Lua %s', 'restored wrong native VM')
            assert(address==computer.tmpAddress() and fs.address==address, 'tmp identity lost')
            assert(alias==file and fs.read(alias,3)==string.char(0,255)..'z', 'open binary handle/offset lost'); fs.close(file)
            eeprom.setData('verified')
            while true do computer.pullSignal() end
            """.formatted(version, version, Boolean.getBoolean("neoopencomputers.testApiSmokeAddon"), addonName, version, version);
    }
}
