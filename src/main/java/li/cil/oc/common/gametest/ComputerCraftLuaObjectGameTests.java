package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ComputerCraftLuaObjectGameTests {
    private static final String VALUE_CLASS = "li.cil.oc.common.integration.ComputerCraftLuaObjectValue";

    @GameTest(template = "empty")
    public static void expiredComputerCraftValueLoadsWithoutForeignRuntime(GameTestHelper helper) throws Exception {
        final var value = (Value) Class.forName(VALUE_CLASS).getDeclaredConstructor().newInstance();
        value.load(new CompoundTag());
        final var peripheral = (ManagedPeripheral) value;
        helper.assertTrue(peripheral.methods().length == 0, "Expired CC value exposed live methods");
        final var result = peripheral.invoke("oldMethod", null, arguments());
        helper.assertTrue(result.length == 2 && result[0] == null && result[1].equals("ComputerCraft userdata cannot be persisted"), "Restored CC value did not explain its expired context");
        helper.succeed();
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        return Loaded.tests();
    }

    private static Arguments arguments(Object... values) {
        return (Arguments) java.lang.reflect.Proxy.newProxyInstance(Arguments.class.getClassLoader(), new Class<?>[]{Arguments.class}, (proxy, method, args) -> switch (method.getName()) {
            case "count" -> values.length;
            case "checkAny" -> values[(int) args[0]];
            case "toArray" -> values.clone();
            case "iterator" -> Arrays.asList(values).iterator();
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static final class TestContext extends AbstractManagedEnvironment implements Context, AutoCloseable {
        final List<Object[]> signals = new ArrayList<>();
        TestContext() { setNode(Network.newNode(this, Visibility.Network).create()); Network.joinNewNetwork(node()); }
        public boolean canInteract(String player) { return true; }
        public boolean isRunning() { return true; }
        public boolean isPaused() { return false; }
        public boolean start() { return false; }
        public boolean pause(double seconds) { return false; }
        public boolean stop() { return false; }
        public void consumeCallBudget(double cost) { }
        public boolean signal(String name, Object... args) {
            final Object[] signal = new Object[args.length + 1]; signal[0] = name;
            System.arraycopy(args, 0, signal, 1, args.length); signals.add(signal); return true;
        }
        public void close() { node().remove(); }
    }

    public static final class AnnotatedObject {
        private final net.minecraft.server.MinecraftServer server;
        final AtomicInteger calls = new AtomicInteger();
        AnnotatedObject(net.minecraft.server.MinecraftServer server) { this.server = server; }
        @dan200.computercraft.api.lua.LuaFunction({"echo", "alias"})
        public String echo(String value) { return value; }
        @dan200.computercraft.api.lua.LuaFunction(mainThread = true)
        public int task() {
            if (!server.isSameThread()) throw new IllegalStateException("Annotated object ran off-thread");
            return calls.incrementAndGet();
        }
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(test("cc_luaobject_dynamic_codec", Loaded::dynamic), test("cc_luaobject_annotated", Loaded::annotated),
                test("cc_luaobject_tasks", Loaded::tasks), test("cc_luaobject_task_callback_and_yield", Loaded::callbacks),
                test("cc_luaobject_persistence", Loaded::persistence), test("cc_luaobject_thread_guard", Loaded::threadGuard),
                test("cc_luaobject_native_adapter_and_cc_handle", h -> nativeAdapter(h, true)),
                test("cc_luaobject_luaj_adapter_and_cc_handle", h -> nativeAdapter(h, false)),
                test("cc_luaobject_collection_binary", Loaded::collectionBinary), test("cc_luaobject_fifo_callback", Loaded::fifoCallback));
        }
        interface Body { void run(GameTestHelper helper) throws Exception; }
        static TestFunction test(String name, Body body) {
            return new TestFunction("ccLuaObjects", name, "neoopencomputers:ae2_export", 200, 0, true,
                helper -> { try { body.run(helper); } catch (Exception failure) { throw new RuntimeException(failure); } });
        }
        static ManagedPeripheral converted(GameTestHelper helper, Object object) {
            final Object result = OpenComputersApi.convert(new Object[]{object})[0];
            helper.assertTrue(result instanceof Map<?, ?> && ((Map<?, ?>) result).get("value") instanceof Value
                && ((Map<?, ?>) result).get("value") instanceof ManagedPeripheral, "CC Lua object is missing its callable OC value wrapper: " + result);
            return (ManagedPeripheral) ((Map<?, ?>) result).get("value");
        }
        interface Invocation { void run() throws Exception; }
        static void error(GameTestHelper h, Invocation invocation, String message) {
            try { invocation.run(); }
            catch (Exception failure) { h.assertTrue(failure.getMessage() != null && failure.getMessage().contains(message), "Wrong Lua object error: " + failure); return; }
            h.fail("Lua object call did not fail: " + message);
        }
        static dan200.computercraft.api.lua.IDynamicLuaObject object(String[] names, DynamicCall body) {
            return new dan200.computercraft.api.lua.IDynamicLuaObject() {
                public String[] getMethodNames() { return names; }
                public dan200.computercraft.api.lua.MethodResult callMethod(dan200.computercraft.api.lua.ILuaContext context, int method, dan200.computercraft.api.lua.IArguments args) throws dan200.computercraft.api.lua.LuaException {
                    return body.run(context, method, args);
                }
            };
        }
        interface DynamicCall {
            dan200.computercraft.api.lua.MethodResult run(dan200.computercraft.api.lua.ILuaContext context, int method, dan200.computercraft.api.lua.IArguments args) throws dan200.computercraft.api.lua.LuaException;
        }
        static void dynamic(GameTestHelper h) throws Exception {
            final var object = object(new String[]{"echo", "fail"}, (context, method, args) -> {
                if (method == 1) throw new dan200.computercraft.api.lua.LuaException("object failure");
                final var bytes = args.getBytes(1); final byte[] copy = new byte[bytes.remaining()]; bytes.get(copy);
                return dan200.computercraft.api.lua.MethodResult.of(args.getString(0), copy, args.get(2));
            });
            final var value = converted(h, object);
            try (var context = new TestContext()) {
                final byte[] bytes = {0, (byte) 128, (byte) 255};
                final var nested = Map.of("text", "héllo".getBytes(StandardCharsets.UTF_8));
                final var result = value.invoke("echo", context, arguments("héllo".getBytes(StandardCharsets.UTF_8), bytes, nested));
                h.assertTrue(result[0].equals("héllo") && Arrays.equals((byte[]) result[1], bytes)
                    && ((Map<?, ?>) result[2]).get("text").equals("héllo"), "CC object argument/result codec lost Unicode, raw bytes or nested text");
                error(h, () -> value.invoke("fail", context, arguments()), "object failure");
                error(h, () -> value.invoke("missing", context, arguments()), "method");
            } finally { ((Value) value).dispose(null); }
            h.succeed();
        }
        static void annotated(GameTestHelper h) throws Exception {
            final var object = new AnnotatedObject(h.getLevel().getServer()); final var value = converted(h, object);
            try (var context = new TestContext()) {
                h.assertTrue(Set.of(value.methods()).containsAll(Set.of("echo", "alias", "task")), "Annotated aliases or main-thread method were omitted");
                h.assertTrue(value.invoke("alias", context, arguments("héllo"))[0].equals("héllo"), "Annotated alias result changed");
                h.assertTrue(((Number) value.invoke("task", context, arguments())[0]).intValue() == 1 && object.calls.get() == 1, "Annotated main-thread task did not execute exactly once");
            } finally { ((Value) value).dispose(null); }
            h.succeed();
        }

        static void collectionBinary(GameTestHelper h) throws Exception {
            final var buffer = java.nio.ByteBuffer.wrap(new byte[]{9, 0, (byte) 128, (byte) 255, 42, 9});
            buffer.position(1); buffer.limit(5);
            final var data = List.of(buffer, Map.of("inner", List.of(buffer)));
            final var value = converted(h, object(new String[]{"collection"}, (context, method, args) -> dan200.computercraft.api.lua.MethodResult.of((Object) data)));
            try (var context = new TestContext()) {
                final var result = OpenComputersApi.convert(value.invoke("collection", context, arguments()));
                final var entries = (Object[]) result[0];
                final var nested = (Object[]) ((Map<?, ?>) entries[1]).get("inner");
                final byte[] expected = {0, (byte) 128, (byte) 255, 42};
                h.assertTrue(entries[0] instanceof byte[] && Arrays.equals((byte[]) entries[0], expected)
                    && nested[0] instanceof byte[] && Arrays.equals((byte[]) nested[0], expected), "Collection-shaped CC results lost raw binary data");
                h.assertTrue(buffer.position() == 1 && buffer.limit() == 5, "Collection conversion moved the foreign ByteBuffer cursor");
            } finally { ((Value) value).dispose(null); }
            h.succeed();
        }

        static void fifoCallback(GameTestHelper h) throws Exception {
            final var tail = new AtomicInteger(); final var ids = new long[2];
            final var value = converted(h, object(new String[]{"ordered"}, (context, method, args) -> {
                ids[0] = context.issueMainThreadTask(() -> new Object[]{"ordered-first"});
                ids[1] = context.issueMainThreadTask(() -> { tail.incrementAndGet(); return new Object[]{"ordered-tail"}; });
                return dan200.computercraft.api.lua.MethodResult.pullEvent("task_complete", event -> {
                    if (((Number) event[1]).longValue() != ids[0]) throw new dan200.computercraft.api.lua.LuaException("Callback tasks completed out of issue order");
                    return dan200.computercraft.api.lua.MethodResult.of(event[3]);
                });
            }));
            final var context = new TestContext();
            try {
                final var result = value.invoke("ordered", context, arguments());
                h.assertTrue(result[0].equals("ordered-first") && tail.get() == 0, "Callback eagerly ran its later fire-and-forget task");
                h.runAfterDelay(3, () -> {
                    try {
                        h.assertTrue(tail.get() == 1 && context.signals.size() == 1, "Tail task was lost, duplicated or consumed as the awaited result");
                        final var event = context.signals.getFirst();
                        h.assertTrue(event[0].equals("task_complete") && ((Number) event[1]).longValue() == ids[1]
                            && Boolean.TRUE.equals(event[2]) && event[3].equals("ordered-tail"), "Tail completion was delivered to the wrong callback/context");
                        h.succeed();
                    } finally { ((Value) value).dispose(null); context.close(); }
                });
            } catch (Exception failure) { ((Value) value).dispose(null); context.close(); throw failure; }
        }
        static void tasks(GameTestHelper h) throws Exception {
            final var calls = new AtomicInteger(); final var tick = new long[]{-1};
            final var value = converted(h, object(new String[]{"async"}, (context, method, args) -> dan200.computercraft.api.lua.MethodResult.of(context.issueMainThreadTask(() -> {
                h.assertTrue(h.getLevel().getServer().isSameThread(), "Async object task ran off-thread");
                tick[0] = h.getLevel().getGameTime(); calls.incrementAndGet(); return new Object[]{new byte[]{0, (byte) 255}, 731};
            }))));
            final var context = new TestContext(); final long before = h.getLevel().getGameTime();
            final long id = ((Number) value.invoke("async", context, arguments())[0]).longValue();
            h.assertTrue(calls.get() == 0, "Asynchronous task executed inside its issuing call");
            h.runAfterDelay(3, () -> {
                try {
                    h.assertTrue(calls.get() == 1 && tick[0] > before && context.signals.size() == 1, "Async task did not complete once on a later tick");
                    final var signal = context.signals.getFirst();
                    h.assertTrue(signal[0].equals("task_complete") && ((Number) signal[1]).longValue() == id && Boolean.TRUE.equals(signal[2])
                        && Arrays.equals((byte[]) signal[3], new byte[]{0, (byte) 255}) && ((Number) signal[4]).intValue() == 731, "Task completion payload changed");
                    value.invoke("async", context, arguments()); ((Value) value).dispose(context);
                    h.runAfterDelay(3, () -> { try { h.assertTrue(calls.get() == 1 && context.signals.size() == 1, "Disposed CC value executed pending work"); h.succeed(); } finally { context.close(); } });
                } catch (Exception failure) { context.close(); throw new RuntimeException(failure); }
            });
        }
        static void callbacks(GameTestHelper h) throws Exception {
            final var calls = new AtomicInteger();
            final var value = converted(h, object(new String[]{"task", "wait"}, (context, method, args) -> {
                if (method == 1) return dan200.computercraft.api.lua.MethodResult.pullEvent("foreign_event", event -> dan200.computercraft.api.lua.MethodResult.of("unexpected"));
                final dan200.computercraft.api.lua.ILuaContext defaultContext = task -> context.issueMainThreadTask(task);
                return defaultContext.executeMainThreadTask(() -> { calls.incrementAndGet(); return new Object[]{"task-result", new byte[]{(byte) 128}}; });
            }));
            try (var context = new TestContext()) {
                final var result = value.invoke("task", context, arguments());
                h.assertTrue(result[0].equals("task-result") && Arrays.equals((byte[]) result[1], new byte[]{(byte) 128}) && calls.get() == 1, "Owned task callback lost results or repeated work");
                error(h, () -> value.invoke("wait", context, arguments()), "event");
            } finally { ((Value) value).dispose(null); }
            h.succeed();
        }
        static void persistence(GameTestHelper h) throws Exception {
            final var live = converted(h, object(new String[]{"echo"}, (context, method, args) -> dan200.computercraft.api.lua.MethodResult.of("live")));
            try {
                final var saved = new CompoundTag(); ((Value) live).save(saved);
                final var restored = (Value) live.getClass().getDeclaredConstructor().newInstance(); restored.load(saved);
                h.assertTrue(((ManagedPeripheral) restored).methods().length == 0, "Persisted foreign object still exposed methods");
                final var result = ((ManagedPeripheral) restored).invoke("echo", null, arguments());
                h.assertTrue(result[0] == null && result[1].equals("ComputerCraft userdata cannot be persisted"), "Expired value did not return its legacy error");
            } finally { ((Value) live).dispose(null); }
            h.succeed();
        }
        static void threadGuard(GameTestHelper h) {
            final var object = new AnnotatedObject(h.getLevel().getServer()); final var value = converted(h, object);
            final var context = new TestContext();
            final var background = CompletableFuture.runAsync(() -> {
                try { value.invoke("task", context, arguments()); throw new AssertionError("Off-thread object invocation was accepted"); }
                catch (li.cil.oc.api.machine.LimitReachedException expected) { }
                catch (Exception failure) { throw new RuntimeException(failure); }
            });
            h.startSequence().thenWaitUntil(() -> h.assertTrue(background.isDone(), "Waiting for object thread guard"))
                .thenExecute(() -> { try { background.join(); h.assertTrue(object.calls.get() == 0, "Off-thread invocation had a side effect"); } finally { ((Value) value).dispose(null); context.close(); } })
                .thenSucceed();
        }

        static void nativeAdapter(GameTestHelper h, boolean nativeLua) throws Exception {
            final var pos = h.absolutePos(new net.minecraft.core.BlockPos(4, 2, 2));
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState());
            final var probe = new TestComputerCraftLuaObjectPeripheral(h.getLevel());
            final var fixture = TestComputerCraftCapabilities.attach(h.getLevel(), pos, net.minecraft.core.Direction.WEST, probe);
            final var adapterPos = pos.west();
            h.getLevel().setBlockAndUpdate(adapterPos, li.cil.oc.common.ModBlocks.ADAPTER.get().defaultBlockState());
            final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) h.getLevel().getBlockEntity(adapterPos);
            adapter.onLoad(); adapter.refreshNeighbors();
            final var computerPos = adapterPos.west();
            h.getLevel().setBlockAndUpdate(computerPos, li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            final var computer = (li.cil.oc.common.blockentity.ComputerCaseBlockEntity) h.getLevel().getBlockEntity(computerPos);
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu,
                nativeLua ? li.cil.oc.common.machine.NativeLua53Architecture.class : li.cil.oc.common.machine.LuaArchitecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local p=component.proxy(assert(component.list('oc_lua_object_probe')(), 'object peripheral missing'))
                local o=p.object().value
                assert(o.echo('héllo')=='héllo')
                local bytes=string.char(0,128,255,42)
                assert(o.binary(bytes)==bytes and o.sync()==1)
                local result,reason=o.fail(); assert(result==nil and tostring(reason):find('native object failure',1,true))
                local a=p.annotated().value
                assert(a.alias('héllo')=='héllo' and a.task()==1)
                local h=p.handle().value
                assert(h.read(2)==string.char(0,128) and h.seek('set',0)==0)
                assert(h.readAll()==bytes); h.close()
                local result,reason=h.read(1); assert(result==nil and tostring(reason):find('closed',1,true))
                local values,label=p.callback()
                assert(values,tostring(label))
                assert(values[1]==bytes and values[2].inner[1]==bytes and label=='callback-results','peripheral callback lost binary collection results')
                assert(p.ordered()=='ordered-first','peripheral task callback order changed')
                local collection=o.collection()
                assert(collection[1]==bytes and collection[2].inner[1]==bytes,'Lua object collection lost binary data')
                local peripheralTask=p.issue(); local objectTask=o.async()
                assert(peripheralTask~=objectTask,'peripheral and object task IDs collide')
                local seenPeripheral,seenObject=false,false
                while not (seenPeripheral and seenObject) do
                  local event,id,ok,value,binary=computer.pullSignal(1)
                  if event=='task_complete' and id==peripheralTask then assert(ok and value=='peripheral-task'); seenPeripheral=true end
                  if event=='task_complete' and id==objectTask then assert(ok and value=='object-task' and binary==string.char(0,255)); seenObject=true end
                end
                assert(p.tailCount()==1,'peripheral tail task was lost or duplicated')
                component.proxy(component.list('eeprom')()).setData('cc-object-native-verified')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            h.assertTrue(computer.machine().start(), "Native object computer did not start");
            h.startSequence().thenWaitUntil(() -> {
                final var marker = new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
                h.assertTrue(marker.equals("cc-object-native-verified"), "Native adapter/object/CC handle program failed: " + computer.machine().lastError());
            }).thenExecute(() -> {
                computer.machine().stop(); probe.close();
                try { fixture.close(); } catch (Exception failure) { throw new RuntimeException(failure); }
            }).thenSucceed();
        }
    }
}
