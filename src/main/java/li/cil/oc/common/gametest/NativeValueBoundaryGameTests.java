package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.LuaArchitecture;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import li.cil.oc.common.machine.NativeLua53Architecture;
import li.cil.oc.common.machine.NativeLua54Architecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class NativeValueBoundaryGameTests {
    @GameTest(template = "empty", timeoutTicks = 400) public static void luaJReceivesNbtSignalAndComponentGraph(GameTestHelper h) { verify(h, LuaArchitecture.class, false); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua52ReceivesNbtSignalAndComponentGraph(GameTestHelper h) { verify(h, NativeLuaArchitecture.class, false); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua53ReceivesNbtSignalAndComponentGraph(GameTestHelper h) { verify(h, NativeLua53Architecture.class, false); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua54ReceivesNbtSignalAndComponentGraph(GameTestHelper h) { verify(h, NativeLua54Architecture.class, false); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua52ReloadsGraphAndQueuedNbtSignal(GameTestHelper h) { verify(h, NativeLuaArchitecture.class, true); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua53ReloadsGraphAndQueuedNbtSignal(GameTestHelper h) { verify(h, NativeLua53Architecture.class, true); }
    @GameTest(template = "empty", timeoutTicks = 400) public static void lua54ReloadsGraphAndQueuedNbtSignal(GameTestHelper h) { verify(h, NativeLua54Architecture.class, true); }

    public static final class GraphEnvironment extends AbstractManagedEnvironment {
        private final Map<String, Object> graph = new LinkedHashMap<>();
        GraphEnvironment() {
            final var child = Map.of("value", 731);
            final Object[] shared = {child, child};
            graph.put("self", graph); graph.put("shared", shared); graph.put("same", shared); graph.put("child", child);
            graph.put("ints", new int[]{-2, 731}); graph.put("longs", new long[]{3, -4});
            graph.put("chars", new char[]{'x', '水'}); graph.put("character", '水');
            graph.put("booleans", new boolean[]{true, false}); graph.put("bytes", new byte[]{0, (byte) 128, (byte) 255, 42});
            setNode(Network.newNode(this, Visibility.Network).withComponent("value_graph", Visibility.Network).create());
        }
        @Callback(direct = true) public Object[] get(Context context, Arguments args) { return new Object[]{graph}; }
    }

    private static void verify(GameTestHelper helper, Class<? extends Architecture> architecture, boolean reload) {
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        final var level = helper.getLevel();
        level.setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var computer = (ComputerCaseBlockEntity) level.getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, architecture);
        computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
        final var graph = new GraphEnvironment(); Network.joinNewNetwork(graph.node());
        computer.onLoad(); Network.joinOrCreateNetwork(computer); computer.node().connect(graph.node());
        final String program = """
            local eeprom=component.proxy(component.list('eeprom')())
            local retained=731
            local graph
            if %s then graph=component.invoke('%s','get') end
            assert(eeprom.getData()=='','value computer rebooted')
            eeprom.setData('values-ready')
            local event,tag,character,flat
            repeat event,tag,character,flat=computer.pullSignal() until event=='values_probe'
            assert(retained==731,'retained VM local lost')
            assert(type(tag)=='table' and tag.ints[1]==-2 and tag.ints[2]==731,'NBT int array lost')
            assert(tag.longs[1]==3 and tag.longs[2]==-4 and tag.child.text=='héllo','NBT long/compound lost')
            assert(tag.bytes==string.char(0,128,255,42) and tag.rows[1].n==7 and tag.rows[2].n==9,'NBT byte/list data lost')
            assert(character==120 and flat.plain=='text','signal normalization changed')
            if %s then
              assert(flat['7']=='seven' and type(flat.nbt)=='string','legacy saved-map representation changed')
            else
              assert(flat[7]=='seven' and flat.nbt.text=='héllo','live Java-map conversion changed')
              graph=component.invoke('%s','get')
            end
            assert(rawequal(graph,graph.self) and rawequal(graph.shared,graph.same),'component graph identity lost')
            assert(rawequal(graph.shared[1],graph.child) and rawequal(graph.shared[1],graph.shared[2]),'shared child lost')
            assert(graph.child.value==731 and graph.ints[1]==-2 and graph.ints[2]==731,'component array/table values changed')
            assert(graph.longs[1]==3 and graph.longs[2]==-4 and graph.booleans[1] and not graph.booleans[2])
            assert(graph.chars[1]=='x' and graph.chars[2]=='水' and graph.character=='水','component character values changed')
            assert(graph.bytes==string.char(0,128,255,42),'component byte array changed')
            eeprom.setData('values-verified')
            while true do computer.pullSignal() end
            """.formatted(reload, graph.node().address(), reload, graph.node().address());
        computer.setItem(6, RobotMovementPersistenceGameTests.eeprom(program));
        ((Connector) computer.machine().node()).changeBuffer(10000);
        helper.assertTrue(computer.machine().start(), "Value boundary computer did not start");
        final ComputerCaseBlockEntity[] active = {computer};
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(marker(computer).equals("values-ready"), "Value computer did not reach ready: " + computer.machine().lastError()))
            .thenExecute(() -> {
                final var data = new CompoundTag();
                data.putIntArray("ints", new int[]{-2, 731}); data.putLongArray("longs", new long[]{3, -4});
                data.putByteArray("bytes", new byte[]{0, (byte) 128, (byte) 255, 42});
                final var child = new CompoundTag(); child.putString("text", "héllo"); data.put("child", child);
                final var rows = new ListTag();
                for (int n : new int[]{7, 9}) { final var row = new CompoundTag(); row.putInt("n", n); rows.add(row); }
                data.put("rows", rows);
                final Map<Object, Object> flat = new LinkedHashMap<>(); flat.put("plain", "text"); flat.put(7, "seven"); flat.put("nbt", child);
                helper.assertTrue(computer.machine().signal("values_probe", data, 'x', flat), "NBT signal was not queued");
                if (reload) {
                    computer.onChunkUnloaded();
                    final var saved = computer.saveWithFullMetadata(level.registryAccess()); computer.setRemoved();
                    helper.assertTrue(!computer.machine().architecture().isInitialized(), "Unload kept the old value VM alive");
                    active[0] = (ComputerCaseBlockEntity) BlockEntity.loadStatic(pos, computer.getBlockState(), saved, level.registryAccess());
                    level.setBlockEntity(active[0]); active[0].onLoad();
                    helper.assertTrue(active[0].machine().isRunning(), "Restored value VM stopped");
                }
            }).thenWaitUntil(() -> {
                ((Connector) active[0].machine().node()).changeBuffer(10000);
                helper.assertTrue(marker(active[0]).equals("values-verified"), "Signal/component graph failed: " + active[0].machine().lastError());
            }).thenExecute(() -> { active[0].machine().stop(); graph.node().remove(); }).thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
