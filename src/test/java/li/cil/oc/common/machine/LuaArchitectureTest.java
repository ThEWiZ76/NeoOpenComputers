package li.cil.oc.common.machine;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.MachineRegistry;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.component.DataCardEnvironment;
import li.cil.oc.common.component.EepromEnvironment;
import li.cil.oc.common.component.GraphicsCardEnvironment;
import li.cil.oc.common.component.ScreenEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class LuaArchitectureTest {
    @AfterEach
    void resetApi() {
        API.driver = null;
        API.fileSystem = null;
        API.items = null;
        API.machine = null;
        API.manual = null;
        API.nanomachines = null;
        API.network = null;
        li.cil.oc.api.Machine.LuaArchitecture = null;
        ProgramLocations.clear();
    }

    @Test
    void executesConfiguredLuaChunkOnce() {
        LuaArchitecture architecture = new LuaArchitecture("counter = (counter or 0) + 1");

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(1, architecture.globalInteger("counter"));
    }

    @Test
    void reportsInitializedOnlyAfterInitialBootRun() {
        LuaArchitecture architecture = new LuaArchitecture("counter = 1");

        assertTrue(architecture.initialize());
        assertEquals(false, architecture.isInitialized());

        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.isInitialized());
    }

    @Test
    void doesNotReportInitializedDuringInitialBootCallbacks() {
        boolean[] initializedDuringInvoke = {true};
        LuaArchitecture architecture = new LuaArchitecture("result = component.invoke('fs-address', 'label')");
        architecture.bind(machineWithInvokeInitializationCapture(Map.of("fs-address", "filesystem"), architecture, initializedDuringInvoke));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, initializedDuringInvoke[0]);
        assertEquals(true, architecture.isInitialized());
        assertEquals("tmp", architecture.globalString("result"));
    }

    @Test
    void reportsLuaRuntimeErrorsAsExecutionErrors() {
        LuaArchitecture architecture = new LuaArchitecture("error('boot failed')");

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);

        ExecutionResult.Error error = assertInstanceOf(ExecutionResult.Error.class, result);
        assertTrue(error.message.contains("boot failed"));
    }

    @Test
    void exposesSafeLuaStandardLibraries() {
        LuaArchitecture architecture = new LuaArchitecture("""
            mathValue = math.floor(2.9)
            textValue = string.upper('ok')
            values = {}
            table.insert(values, 'item')
            coroutineValue = type(coroutine.create(function() end))
            bitValue = bit32.band(7, 3)
            loaded = load('return 4')()
            stringMetatableType = type(getmetatable('text'))
            versionValue = _VERSION
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(2, architecture.globalInteger("mathValue"));
        assertEquals("OK", architecture.globalString("textValue"));
        assertEquals("thread", architecture.globalString("coroutineValue"));
        assertEquals(3, architecture.globalInteger("bitValue"));
        assertEquals(4, architecture.globalInteger("loaded"));
        assertEquals("nil", architecture.globalString("stringMetatableType"));
        assertEquals("Luaj", architecture.globalString("versionValue"));
    }

    @Test
    void rejectsBytecodeLoadWhenDisabledLikeUpstream() {
        LuaArchitecture architecture = new LuaArchitecture("""
            loaded, message = load(string.dump(function() return 7 end))
            loadedType = type(loaded)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("loadedType"));
        assertTrue(architecture.globalString("message").contains("binary"));
    }

    @Test
    void rejectsBytecodeLoadFromReaderWhenDisabledLikeUpstream() {
        LuaArchitecture architecture = new LuaArchitecture("""
            local dumped = string.dump(function() return 7 end)
            loaded, message = load(function()
              return dumped
            end)
            loadedType = type(loaded)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("loadedType"));
        assertTrue(architecture.globalString("message").contains("binary"));
    }

    @Test
    void usesSandboxEnvironmentForNilLoadEnvLikeUpstream() {
        LuaArchitecture architecture = new LuaArchitecture("""
            sentinel = 'visible'
            loaded = load('return sentinel', nil, nil, nil)
            result = loaded()
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("visible", architecture.globalString("result"));
    }

    @Test
    void exposesMinimalDebugTraceback() {
        LuaArchitecture architecture = new LuaArchitecture("""
            tracebackType = type(debug.traceback)
            tracebackText = debug.traceback('boot failed')
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function", architecture.globalString("tracebackType"));
        assertTrue(architecture.globalString("tracebackText").contains("boot failed"));
    }

    @Test
    void exposesSafeDebugGetInfo() {
        LuaArchitecture architecture = new LuaArchitecture("""
            function sample(argument, ...)
              return argument
            end
            getinfoType = type(debug.getinfo)
            info = debug.getinfo(sample)
            infoType = type(info)
            sourceType = type(info.source)
            whatType = type(info.what)
            lineDefinedType = type(info.linedefined)
            functionFieldType = type(info.func)
            sethookType = type(debug.sethook)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function", architecture.globalString("getinfoType"));
        assertEquals("table", architecture.globalString("infoType"));
        assertEquals("string", architecture.globalString("sourceType"));
        assertEquals("string", architecture.globalString("whatType"));
        assertEquals("number", architecture.globalString("lineDefinedType"));
        assertEquals("nil", architecture.globalString("functionFieldType"));
        assertEquals("nil", architecture.globalString("sethookType"));
    }

    @Test
    void exposesTablePackAndUnpackCompatibility() {
        LuaArchitecture architecture = new LuaArchitecture("""
            packed = table.pack('a', nil, 'c')
            packedCount = packed.n
            first = packed[1]
            third = packed[3]
            unpackedFirst, unpackedSecond, unpackedThird = table.unpack(packed, 1, packed.n)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(3, architecture.globalInteger("packedCount"));
        assertEquals("a", architecture.globalString("first"));
        assertEquals("c", architecture.globalString("third"));
        assertEquals("a", architecture.globalString("unpackedFirst"));
        assertEquals("nil", architecture.globalString("unpackedSecond"));
        assertEquals("c", architecture.globalString("unpackedThird"));
    }

    @Test
    void exposesOpenComputersCheckArgCompatibility() {
        LuaArchitecture architecture = new LuaArchitecture("""
            valid = checkArg(1, 'text', 'string')
            invalid, message = pcall(checkArg, 2, 5, 'string', 'nil')
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("valid"));
        assertEquals(false, architecture.globalBoolean("invalid"));
        assertEquals("bad argument #2 (string or nil expected, got number)", architecture.globalString("message"));
    }

    @Test
    void stringFormatStringSpecifierUsesLuaToStringCompatibility() {
        LuaArchitecture architecture = new LuaArchitecture("""
            formatted = string.sub(string.format('%s', {}), 1, 6)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("table:", architecture.globalString("formatted"));
    }

    @Test
    void exposesUnicodeLibraryToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            wide = unicode.char(0x6c34)
            text = 'a' .. wide .. 'b'
            length = unicode.len(text)
            first = unicode.sub(text, 1, 1)
            middle = unicode.sub(text, 2, 2)
            tail = unicode.sub(text, -2, -1)
            reversed = unicode.reverse(text)
            lower = unicode.lower('ABC')
            upper = unicode.upper('abc')
            wideWidth = unicode.charWidth(wide)
            asciiWidth = unicode.charWidth('a')
            wideFlag = unicode.isWide(wide)
            displayWidth = unicode.wlen('ab' .. wide)
            truncated = unicode.wtrunc('ab' .. wide .. 'c', 4)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        String wide = new String(Character.toChars(0x6c34));
        assertEquals(3, architecture.globalInteger("length"));
        assertEquals("a", architecture.globalString("first"));
        assertEquals(wide, architecture.globalString("middle"));
        assertEquals(wide + "b", architecture.globalString("tail"));
        assertEquals("b" + wide + "a", architecture.globalString("reversed"));
        assertEquals("abc", architecture.globalString("lower"));
        assertEquals("ABC", architecture.globalString("upper"));
        assertEquals(2, architecture.globalInteger("wideWidth"));
        assertEquals(1, architecture.globalInteger("asciiWidth"));
        assertEquals(true, architecture.globalBoolean("wideFlag"));
        assertEquals(4, architecture.globalInteger("displayWidth"));
        assertEquals("ab", architecture.globalString("truncated"));
    }

    @Test
    void exposesSystemLibraryToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            bytecode = system.allowBytecode()
            gc = system.allowGC()
            timeout = system.timeout()
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("bytecode"));
        assertEquals(false, architecture.globalBoolean("gc"));
        assertEquals(5D, architecture.globalDouble("timeout"), 0.000_001D);
    }

    @Test
    void hidesHostAccessLuaLibraries() {
        LuaArchitecture architecture = new LuaArchitecture("""
            ioType = type(io)
            osType = type(os)
            osExecuteType = type(os.execute)
            osGetenvType = type(os.getenv)
            osRemoveType = type(os.remove)
            packageType = type(package)
            requireType = type(require)
            luajavaType = type(luajava)
            dofileType = type(dofile)
            loadfileType = type(loadfile)
            printType = type(print)
            collectgarbageType = type(collectgarbage)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("ioType"));
        assertEquals("table", architecture.globalString("osType"));
        assertEquals("nil", architecture.globalString("osExecuteType"));
        assertEquals("nil", architecture.globalString("osGetenvType"));
        assertEquals("nil", architecture.globalString("osRemoveType"));
        assertEquals("nil", architecture.globalString("packageType"));
        assertEquals("nil", architecture.globalString("requireType"));
        assertEquals("nil", architecture.globalString("luajavaType"));
        assertEquals("nil", architecture.globalString("dofileType"));
        assertEquals("nil", architecture.globalString("loadfileType"));
        assertEquals("nil", architecture.globalString("printType"));
        assertEquals("nil", architecture.globalString("collectgarbageType"));
    }

    @Test
    void exposesSandboxedOsTimeApiToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            clock = os.clock()
            time = os.time()
            formatted = os.date('%F %T', 86400)
            date = os.date('*t', 86400)
            day = date.day
            hour = date.hour
            isdstType = type(date.isdst)
            isdst = date.isdst
            fromTable = os.time({year = 1970, month = 1, day = 2, hour = 0, min = 0, sec = 0})
            """);
        architecture.bind(machineWithTimes(4000L, 1.25D));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(1.25D, architecture.globalDouble("clock"), 0.000_001D);
        assertEquals(36_000D, architecture.globalDouble("time"), 0.000_001D);
        assertEquals("1970-01-02 00:00:00", architecture.globalString("formatted"));
        assertEquals(2, architecture.globalInteger("day"));
        assertEquals(0, architecture.globalInteger("hour"));
        assertEquals("boolean", architecture.globalString("isdstType"));
        assertEquals(false, architecture.globalBoolean("isdst"));
        assertEquals(86_400D, architecture.globalDouble("fromTable"), 0.000_001D);
    }

    @Test
    void exposesOsDifftimeLikeUpstream() {
        LuaArchitecture architecture = new LuaArchitecture("difference = os.difftime(120, 45)");

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(75D, architecture.globalDouble("difference"), 0.000_001D);
    }

    @Test
    void validatesOsTimeArgumentsLikeUpstream() {
        LuaArchitecture architecture = new LuaArchitecture("""
            valid, message = pcall(function()
              os.time(5)
            end)
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("valid"));
        assertTrue(architecture.globalString("message").contains("table or nil expected"));
    }

    @Test
    void readsBootSourceFromEepromDataTag() {
        CompoundTag data = new CompoundTag();
        data.putByteArray(ItemRegistry.EEPROM_CODE_TAG, "counter = 7".getBytes(StandardCharsets.UTF_8));
        LuaArchitecture architecture = new LuaArchitecture();

        architecture.configureBootSource(data);
        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(7, architecture.globalInteger("counter"));
    }

    @Test
    void exposesComputerUptimeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("seconds = computer.uptime()");
        architecture.bind(machineWithUptime(12.5D));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(12.5D, architecture.globalDouble("seconds"), 0.000_001D);
    }

    @Test
    void exposesComputerRealTimeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("seconds = computer.realTime()", () -> 12_345L);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(12.345D, architecture.globalDouble("seconds"), 0.000_001D);
    }

    @Test
    void exposesComputerPullSignalToLua() {
        LuaArchitecture architecture = new LuaArchitecture("name, value = computer.pullSignal()");
        architecture.bind(machineWithSignals(new TestSignal("event", new Object[]{"payload"})));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("event", architecture.globalString("name"));
        assertEquals("payload", architecture.globalString("value"));
    }

    @Test
    void suspendsComputerPullSignalUntilSignalArrives() {
        Queue<Signal> signals = new ArrayDeque<>();
        LuaArchitecture architecture = new LuaArchitecture("""
            name, value = computer.pullSignal()
            continued = true
            """);
        architecture.bind(machine(signals, 0D));

        assertTrue(architecture.initialize());
        ExecutionResult firstResult = architecture.runThreaded(false);

        assertInstanceOf(ExecutionResult.Sleep.class, firstResult);
        assertEquals(false, architecture.globalBoolean("continued"));

        signals.add(new TestSignal("event", new Object[]{"payload"}));
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("event", architecture.globalString("name"));
        assertEquals("payload", architecture.globalString("value"));
        assertTrue(architecture.globalBoolean("continued"));
    }

    @Test
    void resumesComputerPullSignalAfterTimeout() {
        Queue<Signal> signals = new ArrayDeque<>();
        double[] uptime = {10D};
        LuaArchitecture architecture = new LuaArchitecture("""
            name = computer.pullSignal(0.5)
            continued = true
            """);
        architecture.bind(machineWithUptime(signals, uptime));

        assertTrue(architecture.initialize());
        ExecutionResult firstResult = architecture.runThreaded(false);

        ExecutionResult.Sleep sleep = assertInstanceOf(ExecutionResult.Sleep.class, firstResult);
        assertEquals(10, sleep.ticks);
        assertEquals(false, architecture.globalBoolean("continued"));

        uptime[0] = 10.25D;
        assertEquals(false, architecture.globalBoolean("continued"));
        ExecutionResult secondResult = architecture.runThreaded(false);
        assertInstanceOf(ExecutionResult.Sleep.class, secondResult);
        assertEquals(false, architecture.globalBoolean("continued"));

        uptime[0] = 10.5D;
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("name"));
        assertTrue(architecture.globalBoolean("continued"));
    }

    @Test
    void exposesComputerPushSignalToLua() {
        String[] signalName = {null};
        Object[][] signalArguments = {null};
        LuaArchitecture architecture = new LuaArchitecture("result = computer.pushSignal('event', 'payload', 7, true)");
        architecture.bind(machineWithSignalCapture(signalName, signalArguments));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("result"));
        assertEquals("event", signalName[0]);
        assertArrayEquals(new Object[]{"payload", 7D, true}, signalArguments[0]);
    }

    @Test
    void computerPushSignalRequiresSignalName() {
        LuaArchitecture architecture = new LuaArchitecture("""
            valid, message = pcall(function()
              computer.pushSignal()
            end)
            """);
        architecture.bind(machineWithSignalCapture(new String[]{null}, new Object[][]{null}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("valid"));
        assertTrue(architecture.globalString("message").contains("string expected"));
    }

    @Test
    void exposesComputerAddressesToLua() {
        LuaArchitecture architecture = new LuaArchitecture("address = computer.address(); tmp = computer.tmpAddress()");
        architecture.bind(machineWithAddress("machine-address"));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("machine-address", architecture.globalString("address"));
        assertEquals("machine-address", architecture.globalString("tmp"));
    }

    @Test
    void exposesComputerBootAddressToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            before = computer.getBootAddress()
            changed = computer.setBootAddress('fs-address')
            after = computer.getBootAddress()
            cleared = computer.setBootAddress()
            final = computer.getBootAddress()
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("before"));
        assertEquals(true, architecture.globalBoolean("changed"));
        assertEquals("fs-address", architecture.globalString("after"));
        assertEquals(true, architecture.globalBoolean("cleared"));
        assertEquals("nil", architecture.globalString("final"));
    }

    @Test
    void persistsComputerBootAddress() {
        LuaArchitecture first = new LuaArchitecture("computer.setBootAddress('fs-address')");

        assertTrue(first.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, first.runThreaded(false));
        CompoundTag tag = new CompoundTag();
        first.save(tag);
        assertEquals("fs-address", tag.getString("bootAddress"));

        LuaArchitecture second = new LuaArchitecture();
        second.load(tag);
        CompoundTag roundTrip = new CompoundTag();
        second.save(roundTrip);

        assertEquals("fs-address", roundTrip.getString("bootAddress"));
    }

    @Test
    void exposesComputerMemoryToLua() {
        DriverRegistry drivers = new DriverRegistry();
        drivers.add(new TestMemoryDriver(192D));
        API.driver = drivers;
        LuaArchitecture architecture = new LuaArchitecture("free = computer.freeMemory(); total = computer.totalMemory()");

        assertTrue(architecture.recomputeMemory(Collections.singletonList(null)));
        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(98_304D, architecture.globalDouble("free"), 0.000_001D);
        assertEquals(196_608D, architecture.globalDouble("total"), 0.000_001D);
    }

    @Test
    void rejectsMemoryRecomputeWithoutRam() {
        LuaArchitecture architecture = new LuaArchitecture();

        assertEquals(false, architecture.recomputeMemory(Collections.emptyList()));
    }

    @Test
    void exposesComputerEnergyToLua() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Connector connector = (Connector) machine.node();
        connector.setLocalBufferSize(20D);
        connector.changeBuffer(-20D);
        connector.changeBuffer(7D);
        LuaArchitecture architecture = new LuaArchitecture("energy = computer.energy(); maxEnergy = computer.maxEnergy()");
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(7D, architecture.globalDouble("energy"), 0.000_001D);
        assertEquals(20D, architecture.globalDouble("maxEnergy"), 0.000_001D);
    }

    @Test
    void exposesRobotFlagToLua() {
        LuaArchitecture regular = new LuaArchitecture("regularRobot = computer.isRobot()");
        regular.bind(machine(new ArrayDeque<>(), 0D));
        LuaArchitecture robot = new LuaArchitecture("robotFlag = computer.isRobot()");
        robot.bind(machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), new String[0], null, null, null, null, robotHost()));

        assertTrue(regular.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, regular.runThreaded(false));
        assertTrue(robot.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, robot.runThreaded(false));

        assertEquals(false, regular.globalBoolean("regularRobot"));
        assertEquals(true, robot.globalBoolean("robotFlag"));
    }

    @Test
    void exposesDeviceInfoToLua() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());
        TestDeviceEnvironment device = new TestDeviceEnvironment("gpu", Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
            DeviceInfo.DeviceAttribute.Product, "Test GPU"
        ));
        machine.node().connect(device.node());
        LuaArchitecture architecture = new LuaArchitecture("""
            devices = computer.getDeviceInfo()
            for address, info in pairs(devices) do
              if info.class == 'display' then
                deviceAddress = address
                product = info.product
              end
            end
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(device.node().address(), architecture.globalString("deviceAddress"));
        assertEquals("Test GPU", architecture.globalString("product"));
    }

    @Test
    void exposesProgramLocationsToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            locations = computer.getProgramLocations()
            locationsType = type(locations)
            first = locations[1]
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("table", architecture.globalString("locationsType"));
        assertEquals("nil", architecture.globalString("first"));
    }

    @Test
    void exposesRegisteredProgramLocationsToLua() {
        ProgramLocations.addMapping("edit", "OpenOS");
        ProgramLocations.addMapping("dig", "Network", "Lua");
        LuaArchitecture architecture = new LuaArchitecture("""
            locations = computer.getProgramLocations()
            firstProgram = locations[1][1]
            firstLabel = locations[1][2]
            secondProgram = locations[2][1]
            secondLabel = locations[2][2]
            """);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("dig", architecture.globalString("firstProgram"));
        assertEquals("Network", architecture.globalString("firstLabel"));
        assertEquals("edit", architecture.globalString("secondProgram"));
        assertEquals("OpenOS", architecture.globalString("secondLabel"));
    }

    @Test
    void exposesComputerArchitectureSelectionToLua() {
        DriverRegistry drivers = new DriverRegistry();
        TestMutableProcessor processor = new TestMutableProcessor(FirstArchitecture.class);
        drivers.add(processor);
        API.driver = drivers;
        MachineRegistry machines = new MachineRegistry();
        machines.add(FirstArchitecture.class);
        machines.add(SecondArchitecture.class);
        API.machine = machines;
        Machine machine = machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), new String[0], null, null, null, null, hostWithComponents(Collections.singletonList(null)));
        LuaArchitecture architecture = new LuaArchitecture("""
            architectures = computer.getArchitectures()
            first = architectures[1]
            second = architectures[2]
            current = computer.getArchitecture()
            changed = computer.setArchitecture('second')
            after = computer.getArchitecture()
            unchanged = computer.setArchitecture('second')
            missing, missingMessage = computer.setArchitecture('missing')
            missingArgValid, missingArgMessage = pcall(function()
              computer.setArchitecture()
            end)
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("first", architecture.globalString("first"));
        assertEquals("second", architecture.globalString("second"));
        assertEquals("first", architecture.globalString("current"));
        assertEquals(true, architecture.globalBoolean("changed"));
        assertEquals("second", architecture.globalString("after"));
        assertEquals(false, architecture.globalBoolean("unchanged"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("unknown architecture", architecture.globalString("missingMessage"));
        assertEquals(false, architecture.globalBoolean("missingArgValid"));
        assertTrue(architecture.globalString("missingArgMessage").contains("string expected"));
    }

    @Test
    void exposesComputerShutdownToLua() {
        LuaArchitecture architecture = new LuaArchitecture("computer.shutdown()");

        assertTrue(architecture.initialize());
        ExecutionResult.Shutdown result = assertInstanceOf(ExecutionResult.Shutdown.class, architecture.runThreaded(false));

        assertEquals(false, result.reboot);
    }

    @Test
    void exposesComputerRebootToLua() {
        LuaArchitecture architecture = new LuaArchitecture("computer.shutdown(true)");

        assertTrue(architecture.initialize());
        ExecutionResult.Shutdown result = assertInstanceOf(ExecutionResult.Shutdown.class, architecture.runThreaded(false));

        assertEquals(true, result.reboot);
    }

    @Test
    void exposesComputerBeepToLua() {
        String[] beepPattern = {null};
        LuaArchitecture architecture = new LuaArchitecture("computer.beep('..-')");
        architecture.bind(machineWithBeep(beepPattern));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("..-", beepPattern[0]);
    }

    @Test
    void exposesNumericComputerBeepToLuaWithUpstreamTiming() {
        List<short[]> beeps = new ArrayList<>();
        LuaArchitecture architecture = new LuaArchitecture("computer.beep(); computer.beep(440, 0.01); computer.beep(1200, 10)");
        architecture.bind(machineWithNumericBeeps(beeps));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(3, beeps.size());
        assertArrayEquals(new short[]{440, 100}, beeps.get(0));
        assertArrayEquals(new short[]{440, 50}, beeps.get(1));
        assertArrayEquals(new short[]{1200, 5000}, beeps.get(2));
    }

    @Test
    void exposesComputerUsersToLua() {
        String[] added = {null};
        String[] removed = {null};
        LuaArchitecture architecture = new LuaArchitecture("first, second = computer.users(); added = computer.addUser('carol'); removed = computer.removeUser('bob')");
        architecture.bind(machineWithUserAccess(new String[]{"alice", "bob"}, added, removed));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("alice", architecture.globalString("first"));
        assertEquals("bob", architecture.globalString("second"));
        assertEquals(true, architecture.globalBoolean("added"));
        assertEquals(true, architecture.globalBoolean("removed"));
        assertEquals("carol", added[0]);
        assertEquals("bob", removed[0]);
    }

    @Test
    void computerUserMutatorsRequireNames() {
        LuaArchitecture architecture = new LuaArchitecture("""
            addValid, addMessage = pcall(function()
              computer.addUser()
            end)
            removeValid, removeMessage = pcall(function()
              computer.removeUser()
            end)
            """);
        architecture.bind(machineWithUserAccess(new String[0], new String[]{null}, new String[]{null}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("addValid"));
        assertTrue(architecture.globalString("addMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("removeValid"));
        assertTrue(architecture.globalString("removeMessage").contains("string expected"));
    }

    @Test
    void exposesComponentListToLua() {
        LuaArchitecture architecture = new LuaArchitecture("components = component.list(); fs = components['fs-address']");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("fs"));
    }

    @Test
    void filtersComponentListForLua() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs-address", "filesystem");
        components.put("gpu-address", "gpu");
        LuaArchitecture architecture = new LuaArchitecture("partial = component.list('system'); fs = partial['fs-address']; exactMiss = component.list('file', true)['fs-address']; exact = component.list('filesystem', true)['fs-address']; gpu = partial['gpu-address']");
        architecture.bind(machineWithComponents(components));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("fs"));
        assertEquals("nil", architecture.globalString("exactMiss"));
        assertEquals("filesystem", architecture.globalString("exact"));
        assertEquals("nil", architecture.globalString("gpu"));
    }

    @Test
    void ignoresBooleanComponentListFiltersLikeUpstreamLuaJ() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs-address", "filesystem");
        components.put("gpu-address", "gpu");
        LuaArchitecture architecture = new LuaArchitecture("all = component.list(true); fs = all['fs-address']; gpu = all['gpu-address']");
        architecture.bind(machineWithComponents(components));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("fs"));
        assertEquals("gpu", architecture.globalString("gpu"));
    }

    @Test
    void iteratesComponentListInLua() {
        LuaArchitecture architecture = new LuaArchitecture("for address, kind in component.list('file', false) do firstAddress = address; firstKind = kind end");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem", "gpu-address", "gpu")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("fs-address", architecture.globalString("firstAddress"));
        assertEquals("filesystem", architecture.globalString("firstKind"));
    }

    @Test
    void exposesComponentTypeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("kind = component.type('fs-address'); missing, missingMessage = component.type('missing')");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("kind"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void exposesComponentAvailabilityToLua() {
        LuaArchitecture architecture = new LuaArchitecture("hasFs = component.isAvailable('filesystem'); hasGpu = component.isAvailable('gpu')");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("hasFs"));
        assertEquals(false, architecture.globalBoolean("hasGpu"));
    }

    @Test
    void resolvesComponentPrefixesAndPrimaryStatusForLua() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs1-address", "filesystem");
        components.put("fs2-address", "filesystem");
        components.put("gpu-address", "gpu");
        LuaArchitecture architecture = new LuaArchitecture("""
            resolved = component.get('fs2', 'filesystem')
            missing, missingMessage = component.get('none', 'filesystem')
            wrongType, wrongTypeMessage = component.get('gpu', 'filesystem')
            component.setPrimary('filesystem', 'fs2-address')
            selectedPrimary = component.isPrimary('fs2-address')
            otherPrimary = component.isPrimary('fs1-address')
            missingPrimary = component.isPrimary('missing')
            shorthand = component.filesystem
            sameShorthand = shorthand == component.getPrimary('filesystem')
            """);
        architecture.bind(machineWithComponentsAndInvokeResult(components, new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("fs2-address", architecture.globalString("resolved"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
        assertEquals("nil", architecture.globalString("wrongType"));
        assertEquals("no such component", architecture.globalString("wrongTypeMessage"));
        assertTrue(architecture.globalBoolean("selectedPrimary"));
        assertEquals(false, architecture.globalBoolean("otherPrimary"));
        assertEquals(false, architecture.globalBoolean("missingPrimary"));
        assertTrue(architecture.globalBoolean("sameShorthand"));
    }

    @Test
    void exposesComponentSlotToLua() {
        LuaArchitecture architecture = new LuaArchitecture("slot = component.slot('fs-address'); missing, missingMessage = component.slot('missing')");
        architecture.bind(machineWithComponentsAndHostSlot(Map.of("fs-address", "filesystem"), "fs-address", 3));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(3, architecture.globalInteger("slot"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void componentLowLevelFunctionsRequireStringArguments() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        LuaArchitecture architecture = new LuaArchitecture("""
            typeValid, typeMessage = pcall(function()
              component.type()
            end)
            getValid, getMessage = pcall(function()
              component.get()
            end)
            getTypeValid, getTypeMessage = pcall(function()
              component.get('fs-address', 1)
            end)
            availableValid, availableMessage = pcall(function()
              component.isAvailable()
            end)
            primaryValid, primaryMessage = pcall(function()
              component.isPrimary()
            end)
            getPrimaryValid, getPrimaryMessage = pcall(function()
              component.getPrimary()
            end)
            setPrimaryValid, setPrimaryMessage = pcall(function()
              component.setPrimary()
            end)
            setPrimaryAddressValid, setPrimaryAddressMessage = pcall(function()
              component.setPrimary('filesystem', 1)
            end)
            slotValid, slotMessage = pcall(function()
              component.slot()
            end)
            methodsValid, methodsMessage = pcall(function()
              component.methods()
            end)
            fieldsValid, fieldsMessage = pcall(function()
              component.fields()
            end)
            docAddressValid, docAddressMessage = pcall(function()
              component.doc()
            end)
            docMethodValid, docMethodMessage = pcall(function()
              component.doc('fs-address')
            end)
            invokeAddressValid, invokeAddressMessage = pcall(function()
              component.invoke()
            end)
            invokeMethodValid, invokeMethodMessage = pcall(function()
              component.invoke('fs-address')
            end)
            proxyValid, proxyMessage = pcall(function()
              component.proxy()
            end)
            """);
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("typeValid"));
        assertTrue(architecture.globalString("typeMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("getValid"));
        assertTrue(architecture.globalString("getMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("getTypeValid"));
        assertTrue(architecture.globalString("getTypeMessage").contains("string or nil expected"));
        assertEquals(false, architecture.globalBoolean("availableValid"));
        assertTrue(architecture.globalString("availableMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("primaryValid"));
        assertTrue(architecture.globalString("primaryMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("getPrimaryValid"));
        assertTrue(architecture.globalString("getPrimaryMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("setPrimaryValid"));
        assertTrue(architecture.globalString("setPrimaryMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("setPrimaryAddressValid"));
        assertTrue(architecture.globalString("setPrimaryAddressMessage").contains("string or nil expected"));
        assertEquals(false, architecture.globalBoolean("slotValid"));
        assertTrue(architecture.globalString("slotMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("methodsValid"));
        assertTrue(architecture.globalString("methodsMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("fieldsValid"));
        assertTrue(architecture.globalString("fieldsMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("docAddressValid"));
        assertTrue(architecture.globalString("docAddressMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("docMethodValid"));
        assertTrue(architecture.globalString("docMethodMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("invokeAddressValid"));
        assertTrue(architecture.globalString("invokeAddressMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("invokeMethodValid"));
        assertTrue(architecture.globalString("invokeMethodMessage").contains("string expected"));
        assertEquals(false, architecture.globalBoolean("proxyValid"));
        assertTrue(architecture.globalString("proxyMessage").contains("string expected"));
    }

    @Test
    void exposesComponentMethodsToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("direct", callback("directCallback"));
        methods.put("accessor", callback("accessorCallback"));
        LuaArchitecture architecture = new LuaArchitecture("methods = component.methods('fs-address'); labelDirect = methods.label; direct = methods.direct; accessor = methods.accessor; missing, missingMessage = component.methods('missing')");
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("labelDirect"));
        assertEquals(true, architecture.globalBoolean("direct"));
        assertEquals("nil", architecture.globalString("accessor"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void exposesComponentFieldsToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("direct", callback("directCallback"));
        methods.put("accessor", callback("accessorCallback"));
        LuaArchitecture architecture = new LuaArchitecture("fields = component.fields('fs-address'); getter = fields.accessor.getter; setter = fields.accessor.setter; direct = fields.direct; label = fields.label; missing, missingMessage = component.fields('missing')");
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("getter"));
        assertEquals(true, architecture.globalBoolean("setter"));
        assertEquals("nil", architecture.globalString("direct"));
        assertEquals("nil", architecture.globalString("label"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void exposesComponentDocumentationToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        LuaArchitecture architecture = new LuaArchitecture("doc = component.doc('fs-address', 'label'); missing = component.doc('fs-address', 'missing'); missingComponent, missingComponentMessage = component.doc('missing', 'label')");
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function():string -- Regular callback.", architecture.globalString("doc"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("nil", architecture.globalString("missingComponent"));
        assertEquals("no such component", architecture.globalString("missingComponentMessage"));
    }

    @Test
    void exposesComponentInvokeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("result = component.invoke('fs-address', 'label', 'arg'); missing, missingMessage = component.invoke('missing', 'label')");
        architecture.bind(machineWithComponentsAndInvokeResult(Map.of("fs-address", "filesystem"), new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void convertsArrayInvokeResultsToLuaTables() {
        LuaArchitecture architecture = new LuaArchitecture("items = component.invoke('fs-address', 'list'); first = items[1]; second = items[2]");
        architecture.bind(machineWithComponentsAndInvokeResult(Map.of("fs-address", "filesystem"), new Object[]{new String[]{"init.lua", "bin"}}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("init.lua", architecture.globalString("first"));
        assertEquals("bin", architecture.globalString("second"));
    }

    @Test
    void roundTripsOpaqueJavaHandlesThroughLua() {
        Object handle = new Object();
        boolean[] handleRoundTripped = {false};
        LuaArchitecture architecture = new LuaArchitecture("handle = component.invoke('fs-address', 'open'); result = component.invoke('fs-address', 'read', handle)");
        architecture.bind(machineWithComponentsAndHandleInvoke(Map.of("fs-address", "filesystem"), handle, handleRoundTripped));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("ok", architecture.globalString("result"));
        assertEquals(true, handleRoundTripped[0]);
    }

    @Test
    void convertsLuaTableArgumentsToJavaMaps() {
        Object[] capturedArgument = {null};
        LuaArchitecture architecture = new LuaArchitecture("result = component.invoke('fs-address', 'configure', {label = 'disk', size = 4})");
        architecture.bind(machineWithComponentsAndArgumentCapture(Map.of("fs-address", "filesystem"), capturedArgument));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("ok", architecture.globalString("result"));
        Map<?, ?> argument = (Map<?, ?>) capturedArgument[0];
        assertEquals("disk", argument.get("label"));
        assertEquals(4D, argument.get("size"));
    }

    @Test
    void mapsComponentInvokeFailuresToLuaResults() {
        LuaArchitecture architecture = new LuaArchitecture("result, message = component.invoke('fs-address', 'bad')");
        architecture.bind(machineWithThrowingInvoke(new IllegalArgumentException("bad argument")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("result"));
        assertEquals("bad argument", architecture.globalString("message"));
    }

    @Test
    void mapsComponentInvokeCallBudgetLimitsToNoLuaValues() {
        LuaArchitecture architecture = new LuaArchitecture("""
            count = select('#', component.invoke('fs-address', 'bad'))
            result, message = component.invoke('fs-address', 'bad')
            """);
        architecture.bind(machineWithThrowingInvoke(new LimitReachedException()));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(0, architecture.globalInteger("count"));
        assertEquals("nil", architecture.globalString("result"));
        assertEquals("nil", architecture.globalString("message"));
    }

    @Test
    void exposesUserdataLibraryForValueHandles() {
        TestValue value = new TestValue();
        LuaArchitecture architecture = new LuaArchitecture("""
            value = component.invoke('fs-address', 'make')
            methods = userdata.methods(value)
            direct = methods.echo
            doc = userdata.doc(value, 'echo')
            invoked = userdata.invoke(value, 'echo', 'payload')
            called = userdata.call(value, 'call')
            applied = userdata.apply(value, 'apply')
            unapplied = userdata.unapply(value, 'unapply')
            metaCalled = value('meta-call')
            metaApplied = value.metaApply
            value.metaUnapply = 'meta-unapply'
            valueType = value.type
            valueString = tostring(value)
            metatableValue = getmetatable(value)
            unexpectedKeys = 0
            for key in pairs(value) do
              if key == 'echo' then
                echoVisible = true
              elseif key ~= 'type' then
                unexpectedKeys = unexpectedKeys + 1
              end
            end
            disposed = userdata.dispose(value)
            invalidValid, invalidMessage = pcall(function()
              userdata.invoke({}, 'echo')
            end)
            """);
        architecture.bind(machineWithValueSupport(value));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("direct"));
        assertEquals("function():string -- Direct callback.", architecture.globalString("doc"));
        assertEquals("invoked:payload", architecture.globalString("invoked"));
        assertEquals("called:call", architecture.globalString("called"));
        assertEquals("applied:apply", architecture.globalString("applied"));
        assertEquals("called:meta-call", architecture.globalString("metaCalled"));
        assertEquals("applied:metaApply", architecture.globalString("metaApplied"));
        assertEquals("userdata", architecture.globalString("valueType"));
        assertEquals("test-value", architecture.globalString("valueString"));
        assertEquals("userdata", architecture.globalString("metatableValue"));
        assertEquals(true, architecture.globalBoolean("echoVisible"));
        assertEquals(0, architecture.globalInteger("unexpectedKeys"));
        assertEquals("nil", architecture.globalString("unapplied"));
        assertTrue(value.unapplied);
        assertEquals("metaUnapply", value.unapplyArgument);
        assertEquals("meta-unapply", value.unapplyValue);
        assertEquals("nil", architecture.globalString("disposed"));
        assertTrue(value.disposed);
        assertEquals(false, architecture.globalBoolean("invalidValid"));
        assertTrue(architecture.globalString("invalidMessage").contains("userdata expected"));
    }

    @Test
    void reusesUserdataProxyForSameValueHandle() {
        TestValue value = new TestValue();
        LuaArchitecture architecture = new LuaArchitecture("""
            first = component.invoke('fs-address', 'make')
            second = component.invoke('fs-address', 'make')
            same = first == second
            keyed = {}
            keyed[first] = 'kept'
            lookup = keyed[second]
            """);
        architecture.bind(machineWithValueSupport(value));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("same"));
        assertEquals("kept", architecture.globalString("lookup"));
    }

    @Test
    void userdataCallbackToStringReturnsDocumentation() {
        TestValue value = new TestValue();
        LuaArchitecture architecture = new LuaArchitecture("""
            value = component.invoke('fs-address', 'make')
            echoType = type(value.echo)
            echoDescription = tostring(value.echo)
            result = value.echo('payload')
            """);
        architecture.bind(machineWithValueSupport(value));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("table", architecture.globalString("echoType"));
        assertEquals("function():string -- Direct callback.", architecture.globalString("echoDescription"));
        assertEquals("invoked:payload", architecture.globalString("result"));
    }

    @Test
    void rejectsStaleUserdataCallbackMethodsLikeUpstream() {
        TestValue value = new TestValue();
        int[] valueInvokes = {0};
        LuaArchitecture architecture = new LuaArchitecture("""
            value = component.invoke('fs-address', 'make')
            valid, message = pcall(function()
              value.echo('payload')
            end)
            """);
        architecture.bind(machineWithDroppedValueMethods(value, valueInvokes));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("valid"));
        assertTrue(architecture.globalString("message").contains("no such method"));
        assertEquals(0, valueInvokes[0]);
    }

    @Test
    void retriesComponentInvokeAfterCallBudgetLimit() {
        int[] attempts = {0};
        LuaArchitecture architecture = new LuaArchitecture("""
            result = component.invoke('fs-address', 'label')
            continued = true
            """);
        architecture.bind(machineWithBudgetRetryInvoke(Map.of("fs-address", "filesystem"), attempts));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(1, attempts[0]);
        assertEquals(false, architecture.globalBoolean("continued"));

        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(2, attempts[0]);
        assertEquals("tmp", architecture.globalString("result"));
        assertEquals(true, architecture.globalBoolean("continued"));
    }

    @Test
    void mapsComponentProxyFailuresToLuaResults() {
        LuaArchitecture architecture = new LuaArchitecture("fs = component.proxy('fs-address'); result, message = fs.bad()");
        architecture.bind(machineWithThrowingInvoke(new IOException("disk failed")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("result"));
        assertEquals("i/o error", architecture.globalString("message"));
    }

    @Test
    void exposesComponentProxyToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        List<String> invokedMethods = new ArrayList<>();
        List<Object[]> invokedArguments = new ArrayList<>();
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            labelType = type(fs.label)
            missingMemberType = type(fs.missing)
            result = fs.label('arg')
            missing, missingMessage = component.proxy('missing')
            """);
        architecture.bind(machineWithMethodsAndInvokeCapture(Map.of("fs-address", "filesystem"), methods, invokedMethods, invokedArguments));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("table", architecture.globalString("labelType"));
        assertEquals("nil", architecture.globalString("missingMemberType"));
        assertEquals(true, architecture.globalBoolean("result"));
        assertEquals(List.of("label"), invokedMethods);
        assertArrayEquals(new Object[]{"arg"}, invokedArguments.getFirst());
        assertEquals("nil", architecture.globalString("missing"));
        assertEquals("no such component", architecture.globalString("missingMessage"));
    }

    @Test
    void componentProxyMethodToStringReturnsDocumentation() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            labelDescription = tostring(fs.label)
            """);
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function():string -- Regular callback.", architecture.globalString("labelDescription"));
    }

    @Test
    void rejectsStaleComponentProxyMethodsLikeUpstream() {
        int[] componentInvokes = {0};
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            valid, message = pcall(function()
              fs.label('payload')
            end)
            """);
        architecture.bind(machineWithDroppedComponentMethods(componentInvokes));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("valid"));
        assertTrue(architecture.globalString("message").contains("no such method"));
        assertEquals(0, componentInvokes[0]);
    }

    @Test
    void exposesComponentProxyFieldsToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("accessor", callback("accessorCallback"));
        List<String> invokedMethods = new ArrayList<>();
        List<Object[]> invokedArguments = new ArrayList<>();
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            value = fs.accessor
            fs.accessor = 'next'
            labelType = type(fs.label)
            """);
        architecture.bind(machineWithMethodsAndInvokeCapture(Map.of("fs-address", "filesystem"), methods, invokedMethods, invokedArguments));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("current", architecture.globalString("value"));
        assertEquals("table", architecture.globalString("labelType"));
        assertEquals(List.of("accessor", "accessor"), invokedMethods);
        assertEquals(0, invokedArguments.get(0).length);
        assertArrayEquals(new Object[]{"next"}, invokedArguments.get(1));
    }

    @Test
    void exposesComponentProxyMethodsToLuaPairs() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("accessor", callback("accessorCallback"));
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            for key, value in pairs(fs) do
              if key == 'label' then
                labelType = type(value)
              end
              if key == 'accessor' then
                accessorGetter = value.getter
              end
              if key == 'fields' then
                fieldsVisible = true
              end
            end
            """);
        architecture.bind(machineWithComponentsAndMethods(Map.of("fs-address", "filesystem"), methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("table", architecture.globalString("labelType"));
        assertEquals(true, architecture.globalBoolean("accessorGetter"));
        assertEquals(false, architecture.globalBoolean("fieldsVisible"));
    }

    @Test
    void exposesComponentProxyMetadataToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("accessor", callback("accessorCallback"));
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.proxy('fs-address')
            again = component.proxy('fs-address')
            primary = component.getPrimary('filesystem')
            address = fs.address
            componentType = fs.type
            slot = fs.slot
            getter = fs.fields.accessor.getter
            setter = fs.fields.accessor.setter
            regularField = fs.fields.label
            sameProxy = fs == again
            samePrimary = fs == primary
            """);
        architecture.bind(machineWithComponentsMethodsAndHostSlot(Map.of("fs-address", "filesystem"), methods, "fs-address", 3));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("fs-address", architecture.globalString("address"));
        assertEquals("filesystem", architecture.globalString("componentType"));
        assertEquals(3, architecture.globalInteger("slot"));
        assertTrue(architecture.globalBoolean("getter"));
        assertTrue(architecture.globalBoolean("setter"));
        assertEquals("nil", architecture.globalString("regularField"));
        assertTrue(architecture.globalBoolean("sameProxy"));
        assertTrue(architecture.globalBoolean("samePrimary"));
    }

    @Test
    void exposesPrimaryComponentProxyToLua() {
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.getPrimary('filesystem')
            result = fs.label()
            missingValid, missingMessage = pcall(function()
              component.getPrimary('gpu')
            end)
            """);
        architecture.bind(machineWithComponentsMethodsAndInvokeResult(Map.of("fs-address", "filesystem"), Map.of("label", callback("labelCallback")), new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
        assertEquals(false, architecture.globalBoolean("missingValid"));
        assertTrue(architecture.globalString("missingMessage").contains("no primary 'gpu' available"));
    }

    @Test
    void setsPrimaryComponentForLua() {
        String[] invokedAddress = {null};
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs1-address", "filesystem");
        components.put("fs2-address", "filesystem");
        components.put("gpu-address", "gpu");
        LuaArchitecture architecture = new LuaArchitecture("""
            selected = component.setPrimary('filesystem', 'fs2')
            wrongTypeValid, wrongTypeMessage = pcall(function()
              component.setPrimary('filesystem', 'gpu')
            end)
            missingValid, missingMessage = pcall(function()
              component.setPrimary('filesystem', 'missing')
            end)
            fs = component.getPrimary('filesystem')
            result = fs.label()
            """);
        architecture.bind(machineWithInvokeCapture(components, invokedAddress));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("selected"));
        assertEquals(false, architecture.globalBoolean("wrongTypeValid"));
        assertTrue(architecture.globalString("wrongTypeMessage").contains("no such component"));
        assertEquals(false, architecture.globalBoolean("missingValid"));
        assertTrue(architecture.globalString("missingMessage").contains("no such component"));
        assertEquals("tmp", architecture.globalString("result"));
        assertEquals("fs2-address", invokedAddress[0]);
    }

    @Test
    void clearingPrimaryComponentEmitsUnavailableSignal() {
        String[] signalName = {null};
        Object[][] signalArguments = {null};
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('filesystem', 'fs-address')
            cleared = component.setPrimary('filesystem', nil)
            """);
        architecture.bind(machineWithComponentsAndSignalCapture(Map.of("fs-address", "filesystem"), signalName, signalArguments));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("nil", architecture.globalString("cleared"));
        assertEquals("component_unavailable", signalName[0]);
        assertArrayEquals(new Object[]{"filesystem"}, signalArguments[0]);
    }

    @Test
    void changingPrimaryComponentDelaysReplacementAvailableSignal() {
        double[] uptime = {12D};
        List<String> signals = new ArrayList<>();
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('filesystem', 'fs1')
            component.setPrimary('filesystem', 'fs2')
            """);
        architecture.bind(machineWithComponentsUptimeAndSignalLog(Map.of(
            "fs1-address", "filesystem",
            "fs2-address", "filesystem"
        ), uptime, signals));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(List.of(
            "component_available:filesystem",
            "component_unavailable:filesystem"
        ), signals);

        uptime[0] = 12.09D;
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
        assertEquals(List.of(
            "component_available:filesystem",
            "component_unavailable:filesystem"
        ), signals);

        uptime[0] = 12.1D;
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
        assertEquals(List.of(
            "component_available:filesystem",
            "component_unavailable:filesystem",
            "component_available:filesystem"
        ), signals);
    }

    @Test
    void componentAddedSignalSelectsPrimaryWhenNoneExists() {
        Map<String, String> components = new LinkedHashMap<>();
        Queue<Signal> signals = new ArrayDeque<>(List.of(new TestSignal("component_added", new Object[]{"fs-address", "filesystem"})));
        LuaArchitecture architecture = new LuaArchitecture("""
            name, address, kind = computer.pullSignal()
            primary = component.getPrimary('filesystem')
            primaryAddress = primary.address
            """);
        architecture.bind(machineWithDynamicComponentsAndSignals(components, signals, () -> components.put("fs-address", "filesystem"), new ArrayList<>()));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("component_added", architecture.globalString("name"));
        assertEquals("fs-address", architecture.globalString("address"));
        assertEquals("filesystem", architecture.globalString("kind"));
        assertEquals("fs-address", architecture.globalString("primaryAddress"));
    }

    @Test
    void componentRemovedSignalSelectsNextPrimaryAndPreservesSignal() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs1-address", "filesystem");
        components.put("fs2-address", "filesystem");
        Queue<Signal> signals = new ArrayDeque<>(List.of(new TestSignal("component_removed", new Object[]{"fs1-address", "filesystem"})));
        List<String> emittedSignals = new ArrayList<>();
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('filesystem', 'fs1')
            name, address, kind = computer.pullSignal()
            available = component.isAvailable('filesystem')
            repeat
              availableName, availableKind = computer.pullSignal()
            until availableName == 'component_available'
            primary = component.getPrimary('filesystem')
            primaryAddress = primary.address
            """);
        double[] uptime = {20D};
        architecture.bind(machineWithDynamicComponentsSignalsUptimeAndSignalLoop(components, signals, uptime, () -> components.remove("fs1-address"), emittedSignals));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("component_removed", architecture.globalString("name"));
        assertEquals("fs1-address", architecture.globalString("address"));
        assertEquals("filesystem", architecture.globalString("kind"));
        assertEquals(false, architecture.globalBoolean("available"));
        assertEquals(List.of(
            "component_available:filesystem",
            "component_unavailable:filesystem"
        ), emittedSignals);

        uptime[0] = 20.1D;
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("component_available", architecture.globalString("availableName"));
        assertEquals("filesystem", architecture.globalString("availableKind"));
        assertEquals("fs2-address", architecture.globalString("primaryAddress"));
        assertEquals(List.of(
            "component_available:filesystem",
            "component_unavailable:filesystem",
            "component_available:filesystem"
        ), emittedSignals);
    }

    @Test
    void screenAddedWithKeyboardReplacesKeyboardlessPrimaryScreen() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("screen1-address", "screen");
        Queue<Signal> signals = new ArrayDeque<>(List.of(new TestSignal("component_added", new Object[]{"screen2-address", "screen"})));
        List<String> emittedSignals = new ArrayList<>();
        double[] uptime = {30D};
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('screen', 'screen1')
            name, address, kind = computer.pullSignal()
            screenAvailableDuringDelay = component.isAvailable('screen')
            keyboardAddress = component.getPrimary('keyboard').address
            repeat
              availableName, availableKind = computer.pullSignal()
            until availableName == 'component_available' and availableKind == 'screen'
            screenAddress = component.getPrimary('screen').address
            """);
        architecture.bind(machineWithScreenKeyboardPrimarySignals(
            components,
            signals,
            uptime,
            () -> {
                components.put("screen2-address", "screen");
                components.put("keyboard2-address", "keyboard");
            },
            Map.of(
                "screen1-address", new Object[0],
                "screen2-address", new Object[]{"keyboard2-address"}
            ),
            emittedSignals
        ));

        assertTrue(architecture.initialize());
        ExecutionResult firstResult = architecture.runThreaded(false);
        if (firstResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, firstResult);

        assertEquals("component_added", architecture.globalString("name"));
        assertEquals("screen2-address", architecture.globalString("address"));
        assertEquals("screen", architecture.globalString("kind"));
        assertEquals(false, architecture.globalBoolean("screenAvailableDuringDelay"));
        assertEquals("keyboard2-address", architecture.globalString("keyboardAddress"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:screen"
        ), emittedSignals);

        uptime[0] = 30.1D;
        ExecutionResult secondResult = architecture.runThreaded(false);
        if (secondResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, secondResult);

        assertEquals("component_available", architecture.globalString("availableName"));
        assertEquals("screen", architecture.globalString("availableKind"));
        assertEquals("screen2-address", architecture.globalString("screenAddress"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:screen",
            "component_available:screen"
        ), emittedSignals);
    }

    @Test
    void keyboardAddedBecomesPrimaryWhenItIsFirstKeyboardOfPrimaryScreen() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("screen-address", "screen");
        components.put("keyboard1-address", "keyboard");
        Queue<Signal> signals = new ArrayDeque<>(List.of(new TestSignal("component_added", new Object[]{"keyboard2-address", "keyboard"})));
        List<String> emittedSignals = new ArrayList<>();
        double[] uptime = {40D};
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('screen', 'screen')
            component.setPrimary('keyboard', 'keyboard1')
            name, address, kind = computer.pullSignal()
            keyboardAvailableDuringDelay = component.isAvailable('keyboard')
            repeat
              availableName, availableKind = computer.pullSignal()
            until availableName == 'component_available' and availableKind == 'keyboard'
            keyboardAddress = component.getPrimary('keyboard').address
            """);
        architecture.bind(machineWithScreenKeyboardPrimarySignals(
            components,
            signals,
            uptime,
            () -> components.put("keyboard2-address", "keyboard"),
            Map.of("screen-address", new Object[]{"keyboard2-address"}),
            emittedSignals
        ));

        assertTrue(architecture.initialize());
        ExecutionResult firstResult = architecture.runThreaded(false);
        if (firstResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, firstResult);

        assertEquals("component_added", architecture.globalString("name"));
        assertEquals("keyboard2-address", architecture.globalString("address"));
        assertEquals("keyboard", architecture.globalString("kind"));
        assertEquals(false, architecture.globalBoolean("keyboardAvailableDuringDelay"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:keyboard"
        ), emittedSignals);

        uptime[0] = 40.1D;
        ExecutionResult secondResult = architecture.runThreaded(false);
        if (secondResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, secondResult);

        assertEquals("component_available", architecture.globalString("availableName"));
        assertEquals("keyboard", architecture.globalString("availableKind"));
        assertEquals("keyboard2-address", architecture.globalString("keyboardAddress"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:keyboard",
            "component_available:keyboard"
        ), emittedSignals);
    }

    @Test
    void removedPrimaryScreenSwitchesKeyboardToNextScreenKeyboard() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("screen1-address", "screen");
        components.put("screen2-address", "screen");
        components.put("keyboard1-address", "keyboard");
        components.put("keyboard2-address", "keyboard");
        Queue<Signal> signals = new ArrayDeque<>(List.of(new TestSignal("component_removed", new Object[]{"screen1-address", "screen"})));
        List<String> emittedSignals = new ArrayList<>();
        double[] uptime = {50D};
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('screen', 'screen1')
            component.setPrimary('keyboard', 'keyboard1')
            name, address, kind = computer.pullSignal()
            screenAvailableDuringDelay = component.isAvailable('screen')
            keyboardAvailableDuringDelay = component.isAvailable('keyboard')
            seenScreen = false
            seenKeyboard = false
            repeat
              availableName, availableKind = computer.pullSignal()
              if availableName == 'component_available' and availableKind == 'screen' then
                seenScreen = true
              end
              if availableName == 'component_available' and availableKind == 'keyboard' then
                seenKeyboard = true
              end
            until seenScreen and seenKeyboard
            screenAddress = component.getPrimary('screen').address
            keyboardAddress = component.getPrimary('keyboard').address
            """);
        architecture.bind(machineWithScreenKeyboardPrimarySignals(
            components,
            signals,
            uptime,
            () -> components.remove("screen1-address"),
            Map.of("screen2-address", new Object[]{"keyboard2-address"}),
            emittedSignals
        ));

        assertTrue(architecture.initialize());
        ExecutionResult firstResult = architecture.runThreaded(false);
        if (firstResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, firstResult);

        assertEquals("component_removed", architecture.globalString("name"));
        assertEquals("screen1-address", architecture.globalString("address"));
        assertEquals("screen", architecture.globalString("kind"));
        assertEquals(false, architecture.globalBoolean("screenAvailableDuringDelay"));
        assertEquals(false, architecture.globalBoolean("keyboardAvailableDuringDelay"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:screen",
            "component_unavailable:keyboard"
        ), emittedSignals);

        uptime[0] = 50.1D;
        ExecutionResult secondResult = architecture.runThreaded(false);
        if (secondResult instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, secondResult);

        assertEquals("screen2-address", architecture.globalString("screenAddress"));
        assertEquals("keyboard2-address", architecture.globalString("keyboardAddress"));
        assertEquals(List.of(
            "component_available:screen",
            "component_available:keyboard",
            "component_unavailable:screen",
            "component_unavailable:keyboard"
        ), emittedSignals.subList(0, 4));
        assertEquals(List.of(
            "component_available:keyboard",
            "component_available:screen"
        ), emittedSignals.subList(4, 6).stream().sorted().toList());
    }

    @Test
    void pairsComponentIncludesPrimaryComponentProxies() {
        LuaArchitecture architecture = new LuaArchitecture("""
            component.setPrimary('filesystem', 'fs')
            apiListType = nil
            primaryAddress = nil
            for key, value in pairs(component) do
              if key == 'list' then
                apiListType = type(value)
              end
              if key == 'filesystem' then
                primaryAddress = value.address
              end
            end
            """);
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function", architecture.globalString("apiListType"));
        assertEquals("fs-address", architecture.globalString("primaryAddress"));
    }

    @Test
    void invokesRealFilesystemComponentFromLua() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        ((Connector) machine.node()).setLocalBufferSize(1);
        ((Connector) machine.node()).changeBuffer(1);
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.getPrimary('filesystem')
            fs.makeDirectory('tmp')
            handle = fs.open('tmp/data.txt', 'w')
            wrote = fs.write(handle, 'hello')
            fs.close(handle)
            handle = fs.open('tmp/data.txt', 'r')
            data = fs.read(handle, 5)
            fs.close(handle)
            entries = fs.list('tmp')
            firstEntry = entries[1]
            exists = fs.exists('tmp/data.txt')
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, result);

        assertEquals(true, architecture.globalBoolean("wrote"));
        assertEquals("hello", architecture.globalString("data"));
        assertEquals("data.txt", architecture.globalString("firstEntry"));
        assertEquals(true, architecture.globalBoolean("exists"));
    }

    @Test
    void bundledLuaBiosBootsInitFromFilesystemComponent() throws IOException {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        int outputHandle = fileSystem.open("init.lua", Mode.Write);
        fileSystem.getHandle(outputHandle).write("bootedFromBios = true".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(fileSystem, "OpenOS", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture(new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8));
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }

        assertInstanceOf(ExecutionResult.Sleep.class, result);
        assertEquals(true, architecture.globalBoolean("bootedFromBios"));
    }

    @Test
    void bundledLuaBiosStoresSelectedFilesystemInEepromData() throws IOException {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        CompoundTag eepromData = new CompoundTag();
        eepromData.putByteArray(ItemRegistry.EEPROM_CODE_TAG, ModEeproms.luaBiosCode());
        EepromEnvironment eeprom = new EepromEnvironment(eepromData);
        FileSystem fileSystem = API.fileSystem.fromMemory(4096);
        int outputHandle = fileSystem.open("init.lua", Mode.Write);
        fileSystem.getHandle(outputHandle).write("bootedFromBios = true".getBytes(StandardCharsets.UTF_8));
        fileSystem.getHandle(outputHandle).close();
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(fileSystem, "OpenOS", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(eeprom.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture(new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8));
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }

        assertEquals(true, architecture.globalBoolean("bootedFromBios"));
        assertEquals(fileSystemEnvironment.node().address(), new String(eepromData.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8));
    }

    @Test
    void eepromBootAddressRoundTripIndexesFilesystemList() throws IOException {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        CompoundTag eepromData = new CompoundTag();
        EepromEnvironment eeprom = new EepromEnvironment(eepromData);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(API.fileSystem.fromMemory(4096), "OpenOS", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(eeprom.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture("""
            local eeprom = component.list('eeprom')()
            local filesystem = component.list('filesystem')()
            component.invoke(eeprom, 'setData', filesystem)
            bootAddress = component.invoke(eeprom, 'getData')
            bootAddressType = type(bootAddress)
            bootAddressKind = component.list('filesystem')[bootAddress]
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }

        assertEquals("string", architecture.globalString("bootAddressType"));
        assertEquals("filesystem", architecture.globalString("bootAddressKind"));
    }

    @Test
    void bundledLuaBiosStartsBundledOpenOsWithoutRuntimeError() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        CompoundTag eepromData = new CompoundTag();
        eepromData.putByteArray(ItemRegistry.EEPROM_CODE_TAG, ModEeproms.luaBiosCode());
        EepromEnvironment eeprom = new EepromEnvironment(eepromData);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(ModLootDisks.openOsFileSystem(), "OpenOS", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(eeprom.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture(new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8));
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        for (int tick = 0; tick < 64; tick++) {
            ExecutionResult result = architecture.runThreaded(false);
            if (result instanceof ExecutionResult.Error error) {
                fail(error.message);
            }
        }
    }

    @Test
    void bundledOpenOsStartsWithGpuAndScreenWithoutRuntimeError() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        CompoundTag eepromData = new CompoundTag();
        eepromData.putByteArray(ItemRegistry.EEPROM_CODE_TAG, ModEeproms.luaBiosCode());
        EepromEnvironment eeprom = new EepromEnvironment(eepromData);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(ModLootDisks.openOsFileSystem(), "OpenOS", null, null, 1);
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        TestTextBuffer screen = new TestTextBuffer();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(eeprom.node());
        machine.node().connect(fileSystemEnvironment.node());
        machine.node().connect(gpu.node());
        machine.node().connect(screen.node());
        LuaArchitecture architecture = new LuaArchitecture(new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8));
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        for (int tick = 0; tick < 64; tick++) {
            ExecutionResult result = architecture.runThreaded(false);
            if (result instanceof ExecutionResult.Error error) {
                fail(error.message);
            }
        }

        assertArrayEquals(new Object[]{screen.node().address()}, gpu.getScreen(null, null));
        assertTrue(screen.hasNonBlankText(), screen.dump());
    }

    @Test
    void luaCanUseDataCardEcKeyMethodsAndPassKeysBackToComponent() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        DataCardEnvironment dataCard = new DataCardEnvironment(2);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(dataCard.node());
        LuaArchitecture architecture = new LuaArchitecture("""
            local address = component.list('data')()
            local data = component.proxy(address)
            local publicKey, privateKey = data.generateKeyPair(256)
            publicType = publicKey:keyType()
            publicFlag = publicKey:isPublic()
            privateFlag = privateKey:isPublic()
            local serialized = publicKey:serialize()
            local signature = data.ecdsa('payload', privateKey)
            verified = data.ecdsa('payload', publicKey, signature)
            local restored = data.deserializeKey(serialized, publicType)
            restoredVerified = data.ecdsa('payload', restored, signature)
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertEquals("ec-public", architecture.globalString("publicType"));
        assertEquals(true, architecture.globalBoolean("publicFlag"));
        assertEquals(false, architecture.globalBoolean("privateFlag"));
        assertEquals(true, architecture.globalBoolean("verified"));
        assertEquals(true, architecture.globalBoolean("restoredVerified"));
    }

    private static Machine machineWithUptime(final double uptime) {
        return machine(new ArrayDeque<>(), uptime);
    }

    private static Machine machineWithSignals(final Signal... signals) {
        return machine(new ArrayDeque<>(Arrays.asList(signals)), 0D);
    }

    private static Machine machineWithSignalCapture(final String[] signalName, final Object[][] signalArguments) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), new String[0], null, null, signalName, signalArguments);
    }

    private static Machine machineWithAddress(final String address) {
        return machine(new ArrayDeque<>(), 0D, address);
    }

    private static Machine machineWithTimes(final long worldTime, final double cpuTime) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "worldTime" -> worldTime;
                case "cpuTime" -> cpuTime;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static MachineHost hostWithComponents(final Iterable<ItemStack> components) {
        return new MachineHost() {
            @Override
            public Machine machine() {
                return null;
            }

            @Override
            public Iterable<ItemStack> internalComponents() {
                return components;
            }

            @Override
            public int componentSlot(final String address) {
                return -1;
            }

            @Override
            public void onMachineConnect(final li.cil.oc.api.network.Node node) {
            }

            @Override
            public void onMachineDisconnect(final li.cil.oc.api.network.Node node) {
            }

            @Override
            public Level world() {
                return null;
            }

            @Override
            public double xPosition() {
                return 0;
            }

            @Override
            public double yPosition() {
                return 0;
            }

            @Override
            public double zPosition() {
                return 0;
            }

            @Override
            public void markChanged() {
            }
        };
    }

    private static MachineHost robotHost() {
        return (MachineHost) Proxy.newProxyInstance(
            Robot.class.getClassLoader(),
            new Class<?>[]{Robot.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-robot-host";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithBeep(final String[] beepPattern) {
        return machine(new ArrayDeque<>(), 0D, null, beepPattern);
    }

    private static Machine machineWithNumericBeeps(final List<short[]> beeps) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "beep" -> {
                    if (args.length == 2) {
                        beeps.add(new short[]{(Short) args[0], (Short) args[1]});
                    }
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithUserAccess(final String[] users, final String[] added, final String[] removed) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), users, added, removed);
    }

    private static Machine machineWithComponents(final Map<String, String> components) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0]);
    }

    private static Machine machineWithInvokeResult(final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), invokeResult);
    }

    private static Machine machineWithComponentsAndInvokeResult(final Map<String, String> components, final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, invokeResult);
    }

    private static Machine machineWithComponentsAndSignalCapture(final Map<String, String> components, final String[] signalName, final Object[][] signalArguments) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0], Map.of(), new String[0], null, null, signalName, signalArguments);
    }

    private static Machine machineWithComponentsAndSignalLog(final Map<String, String> components, final List<String> signals) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "signal" -> {
                    Object[] signalArguments = (Object[]) args[1];
                    signals.add(args[0] + ":" + signalArguments[0]);
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithComponentsUptimeAndSignalLog(final Map<String, String> components, final double[] uptime, final List<String> signals) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime[0];
                case "components" -> components;
                case "signal" -> {
                    Object[] signalArguments = (Object[]) args[1];
                    signals.add(args[0] + ":" + signalArguments[0]);
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithDynamicComponentsAndSignals(final Map<String, String> components, final Queue<Signal> queuedSignals, final Runnable beforePopSignal, final List<String> emittedSignals) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "popSignal" -> {
                    Signal signal = queuedSignals.poll();
                    if (signal != null) {
                        beforePopSignal.run();
                    }
                    yield signal;
                }
                case "signal" -> {
                    Object[] signalArguments = (Object[]) args[1];
                    emittedSignals.add(args[0] + ":" + signalArguments[0]);
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithDynamicComponentsSignalsUptimeAndSignalLoop(final Map<String, String> components, final Queue<Signal> queuedSignals, final double[] uptime, final Runnable beforePopSignal, final List<String> emittedSignals) {
        boolean[] componentRemoved = {false};
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime[0];
                case "components" -> components;
                case "popSignal" -> {
                    Signal signal = queuedSignals.poll();
                    if (signal != null && "component_removed".equals(signal.name())) {
                        beforePopSignal.run();
                        componentRemoved[0] = true;
                    }
                    yield signal;
                }
                case "signal" -> {
                    Object[] signalArguments = (Object[]) args[1];
                    emittedSignals.add(args[0] + ":" + signalArguments[0]);
                    if (componentRemoved[0]) {
                        queuedSignals.add(new TestSignal((String) args[0], signalArguments));
                    }
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithScreenKeyboardPrimarySignals(final Map<String, String> components, final Queue<Signal> queuedSignals, final double[] uptime, final Runnable beforePopSignal, final Map<String, Object[]> keyboardResults, final List<String> emittedSignals) {
        boolean[] componentChanged = {false};
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime[0];
                case "components" -> components;
                case "methods" -> Map.of();
                case "invoke" -> {
                    String address = (String) args[0];
                    String componentMethod = (String) args[1];
                    if ("getKeyboards".equals(componentMethod)) {
                        yield keyboardResults.getOrDefault(address, new Object[0]);
                    }
                    yield new Object[0];
                }
                case "popSignal" -> {
                    Signal signal = queuedSignals.poll();
                    if (signal != null && ("component_added".equals(signal.name()) || "component_removed".equals(signal.name()))) {
                        beforePopSignal.run();
                        componentChanged[0] = true;
                    }
                    yield signal;
                }
                case "signal" -> {
                    Object[] signalArguments = (Object[]) args[1];
                    emittedSignals.add(args[0] + ":" + signalArguments[0]);
                    if (componentChanged[0]) {
                        queuedSignals.add(new TestSignal((String) args[0], signalArguments));
                    }
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithComponentsMethodsAndInvokeResult(final Map<String, String> components, final Map<String, Callback> methods, final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, invokeResult, methods);
    }

    private static Machine machineWithInvokeCapture(final Map<String, String> components, final String[] invokedAddress) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "methods" -> Map.of("label", callback("labelCallback"));
                case "invoke" -> {
                    invokedAddress[0] = (String) args[0];
                    yield new Object[]{"tmp"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithHandleInvoke(final Object handle, final boolean[] handleRoundTripped) {
        return machineWithComponentsAndHandleInvoke(Map.of(), handle, handleRoundTripped);
    }

    private static Machine machineWithComponentsAndHandleInvoke(final Map<String, String> components, final Object handle, final boolean[] handleRoundTripped) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "invoke" -> {
                    final String componentMethod = (String) args[1];
                    final Object[] javaArgs = (Object[]) args[2];
                    if ("open".equals(componentMethod)) {
                        yield new Object[]{handle};
                    }
                    handleRoundTripped[0] = javaArgs.length == 1 && javaArgs[0] == handle;
                    yield handleRoundTripped[0] ? new Object[]{"ok"} : new Object[]{"bad"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithArgumentCapture(final Object[] capturedArgument) {
        return machineWithComponentsAndArgumentCapture(Map.of(), capturedArgument);
    }

    private static Machine machineWithComponentsAndArgumentCapture(final Map<String, String> components, final Object[] capturedArgument) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "invoke" -> {
                    final Object[] javaArgs = (Object[]) args[2];
                    capturedArgument[0] = javaArgs[0];
                    yield new Object[]{"ok"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithInvokeInitializationCapture(final Map<String, String> components, final LuaArchitecture architecture, final boolean[] initializedDuringInvoke) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "invoke" -> {
                    initializedDuringInvoke[0] = architecture.isInitialized();
                    yield new Object[]{"tmp"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithThrowingInvoke(final Exception failure) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("fs-address", "filesystem");
                case "methods" -> Map.of("bad", callback("labelCallback"));
                case "invoke" -> throw failure;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithBudgetRetryInvoke(final Map<String, String> components, final int[] attempts) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "invoke" -> {
                    attempts[0]++;
                    if (attempts[0] == 1) {
                        throw new LimitReachedException();
                    }
                    yield new Object[]{"tmp"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithValueSupport(final TestValue value) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("fs-address", "filesystem");
                case "methods" -> args[0] == value ? Map.of("echo", callback("directCallback")) : Map.of();
                case "invoke" -> {
                    if (args[0] == value) {
                        Object[] javaArgs = (Object[]) args[2];
                        yield new Object[]{"invoked:" + javaArgs[0]};
                    }
                    yield new Object[]{value};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithDroppedValueMethods(final TestValue value, final int[] valueInvokes) {
        boolean[] methodsDropped = {false};
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("fs-address", "filesystem");
                case "methods" -> {
                    if (args[0] == value) {
                        if (methodsDropped[0]) {
                            yield Map.of();
                        }
                        methodsDropped[0] = true;
                        yield Map.of("echo", callback("directCallback"));
                    }
                    yield Map.of();
                }
                case "invoke" -> {
                    if (args[0] == value) {
                        valueInvokes[0]++;
                        yield new Object[]{"invoked"};
                    }
                    yield new Object[]{value};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithDroppedComponentMethods(final int[] componentInvokes) {
        int[] methodReads = {0};
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> Map.of("fs-address", "filesystem");
                case "methods" -> {
                    methodReads[0]++;
                    if (methodReads[0] > 2) {
                        yield Map.of();
                    }
                    yield Map.of("label", callback("directCallback"));
                }
                case "invoke" -> {
                    componentInvokes[0]++;
                    yield new Object[]{"invoked"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithMethods(final Map<String, Callback> methods) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], methods);
    }

    private static Machine machineWithComponentsAndMethods(final Map<String, String> components, final Map<String, Callback> methods) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0], methods);
    }

    private static Machine machineWithMethodsAndInvokeCapture(final Map<String, String> components, final Map<String, Callback> methods, final List<String> invokedMethods, final List<Object[]> invokedArguments) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "methods" -> methods;
                case "invoke" -> {
                    invokedMethods.add((String) args[1]);
                    invokedArguments.add((Object[]) args[2]);
                    yield new Object[]{"accessor".equals(args[1]) && ((Object[]) args[2]).length == 0 ? "current" : true};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithHostSlot(final String address, final int slot) {
        return machineWithComponentsAndHostSlot(Map.of(), address, slot);
    }

    private static Machine machineWithComponentsAndHostSlot(final Map<String, String> components, final String address, final int slot) {
        final MachineHost host = (MachineHost) Proxy.newProxyInstance(
            MachineHost.class.getClassLoader(),
            new Class<?>[]{MachineHost.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "componentSlot" -> address.equals(args[0]) ? slot : -1;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-host";
                default -> defaultValue(method.getReturnType());
            });
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0], Map.of(), new String[0], null, null, null, null, host);
    }

    private static Machine machineWithComponentsMethodsAndHostSlot(final Map<String, String> components, final Map<String, Callback> methods, final String address, final int slot) {
        final MachineHost host = (MachineHost) Proxy.newProxyInstance(
            MachineHost.class.getClassLoader(),
            new Class<?>[]{MachineHost.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "componentSlot" -> address.equals(args[0]) ? slot : -1;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-host";
                default -> defaultValue(method.getReturnType());
            });
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0], methods, new String[0], null, null, null, null, host);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime) {
        return machine(signals, uptime, null);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime, final String address) {
        return machine(signals, uptime, address, null);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime, final String address, final String[] beepPattern) {
        return machine(signals, uptime, address, beepPattern, Map.of(), new Object[0]);
    }

    private static Machine machineWithUptime(final Queue<Signal> signals, final double[] uptime) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime[0];
                case "popSignal" -> signals.poll();
                case "components" -> Map.of();
                case "methods" -> Map.of();
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, Map.of());
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, new String[0], null, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, users, added, removed, null, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed,
        final String[] signalName,
        final Object[][] signalArguments
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, users, added, removed, signalName, signalArguments, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed,
        final String[] signalName,
        final Object[][] signalArguments,
        final MachineHost host
    ) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime;
                case "popSignal" -> signals.poll();
                case "node" -> address == null ? null : nodeWithAddress(address);
                case "tmpAddress" -> address;
                case "host" -> host;
                case "components" -> components;
                case "methods" -> methods;
                case "invoke" -> invokeResult;
                case "users" -> users;
                case "addUser" -> {
                    if (added != null) {
                        added[0] = (String) args[0];
                    }
                    yield null;
                }
                case "removeUser" -> {
                    if (removed != null) {
                        removed[0] = (String) args[0];
                    }
                    yield true;
                }
                case "signal" -> {
                    if (signalName != null) {
                        signalName[0] = (String) args[0];
                    }
                    if (signalArguments != null) {
                        signalArguments[0] = (Object[]) args[1];
                    }
                    yield true;
                }
                case "beep" -> {
                    if (beepPattern != null && args.length == 1) {
                        beepPattern[0] = (String) args[0];
                    }
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Node nodeWithAddress(final String address) {
        return (Node) Proxy.newProxyInstance(
            Node.class.getClassLoader(),
            new Class<?>[]{Node.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "address" -> address;
                case "reachability" -> Visibility.Network;
                case "neighbors", "reachableNodes" -> List.of();
                case "isNeighborOf", "canBeReachedFrom" -> false;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-node";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static final class TestTextBuffer extends AbstractManagedEnvironment implements TextBuffer {
        private int width = 40;
        private int height = 16;
        private int viewportWidth = 40;
        private int viewportHeight = 16;
        private int foreground = 0xFFFFFF;
        private int background;
        private ColorDepth depth = ColorDepth.OneBit;
        private int[][] text = newText(width, height);

        private TestTextBuffer() {
            setNode(ScreenEnvironment.createNode(this));
        }

        @Override public void setEnergyCostPerTick(final double value) {}
        @Override public double getEnergyCostPerTick() { return 0; }
        @Override public void setPowerState(final boolean value) {}
        @Override public boolean getPowerState() { return true; }
        @Override public void setMaximumResolution(final int width, final int height) {}
        @Override public int getMaximumWidth() { return 80; }
        @Override public int getMaximumHeight() { return 25; }
        @Override public void setAspectRatio(final double width, final double height) {}
        @Override public double getAspectRatio() { return 1; }
        @Override public boolean setResolution(final int width, final int height) {
            this.width = width;
            this.height = height;
            viewportWidth = Math.min(viewportWidth, width);
            viewportHeight = Math.min(viewportHeight, height);
            text = newText(width, height);
            return true;
        }
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public boolean setViewport(final int width, final int height) {
            viewportWidth = width;
            viewportHeight = height;
            return true;
        }
        @Override public int getViewportWidth() { return viewportWidth; }
        @Override public int getViewportHeight() { return viewportHeight; }
        @Override public void setMaximumColorDepth(final ColorDepth depth) {}
        @Override public ColorDepth getMaximumColorDepth() { return ColorDepth.OneBit; }
        @Override public boolean setColorDepth(final ColorDepth depth) {
            this.depth = depth;
            return true;
        }
        @Override public ColorDepth getColorDepth() { return depth; }
        @Override public void setPaletteColor(final int index, final int color) {}
        @Override public int getPaletteColor(final int index) { return 0; }
        @Override public void setForegroundColor(final int color) { foreground = color; }
        @Override public void setForegroundColor(final int color, final boolean isFromPalette) { foreground = color; }
        @Override public int getForegroundColor() { return foreground; }
        @Override public boolean isForegroundFromPalette() { return false; }
        @Override public void setBackgroundColor(final int color) { background = color; }
        @Override public void setBackgroundColor(final int color, final boolean isFromPalette) { background = color; }
        @Override public int getBackgroundColor() { return background; }
        @Override public boolean isBackgroundFromPalette() { return false; }
        @Override public void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
            final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
            for (int y = 0; y < snapshot.length; y++) {
                for (int x = 0; x < snapshot[y].length; x++) {
                    snapshot[y][x] = getCodePoint(column + x, row + y);
                }
            }
            for (int y = 0; y < snapshot.length; y++) {
                for (int x = 0; x < snapshot[y].length; x++) {
                    put(column + x + horizontalTranslation, row + y + verticalTranslation, snapshot[y][x]);
                }
            }
        }
        @Override public void fill(final int column, final int row, final int width, final int height, final char value) {
            fill(column, row, width, height, (int) value);
        }
        @Override public void fill(final int column, final int row, final int width, final int height, final int value) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    put(column + x, row + y, value);
                }
            }
        }
        @Override public void set(final int column, final int row, final String value, final boolean vertical) {
            if (value == null) {
                return;
            }
            int offset = 0;
            for (int index = 0; index < value.length(); ) {
                final int codePoint = value.codePointAt(index);
                put(column + (vertical ? 0 : offset), row + (vertical ? offset : 0), codePoint);
                offset++;
                index += Character.charCount(codePoint);
            }
        }
        @Override public char get(final int column, final int row) { return (char) getCodePoint(column, row); }
        @Override public int getCodePoint(final int column, final int row) {
            return isInside(column, row) ? text[row][column] : ' ';
        }
        @Override public int getForegroundColor(final int column, final int row) { return foreground; }
        @Override public boolean isForegroundFromPalette(final int column, final int row) { return false; }
        @Override public int getBackgroundColor(final int column, final int row) { return background; }
        @Override public boolean isBackgroundFromPalette(final int column, final int row) { return false; }
        @Override public void rawSetText(final int column, final int row, final char[][] text) {
            if (text == null) {
                return;
            }
            for (int y = 0; y < text.length; y++) {
                for (int x = 0; x < text[y].length; x++) {
                    put(column + x, row + y, text[y][x]);
                }
            }
        }
        @Override public void rawSetText(final int column, final int row, final int[][] text) {
            if (text == null) {
                return;
            }
            for (int y = 0; y < text.length; y++) {
                for (int x = 0; x < text[y].length; x++) {
                    put(column + x, row + y, text[y][x]);
                }
            }
        }
        @Override public void rawSetForeground(final int column, final int row, final int[][] color) {}
        @Override public void rawSetBackground(final int column, final int row, final int[][] color) {}
        @Override public boolean renderText() { return true; }
        @Override public int renderWidth() { return viewportWidth; }
        @Override public int renderHeight() { return viewportHeight; }
        @Override public void setRenderingEnabled(final boolean enabled) {}
        @Override public boolean isRenderingEnabled() { return true; }
        @Override public void keyDown(final char character, final int code, final Player player) {}
        @Override public void keyUp(final char character, final int code, final Player player) {}
        @Override public void clipboard(final String value, final Player player) {}
        @Override public void mouseDown(final double x, final double y, final int button, final Player player) {}
        @Override public void mouseDrag(final double x, final double y, final int button, final Player player) {}
        @Override public void mouseUp(final double x, final double y, final int button, final Player player) {}
        @Override public void mouseScroll(final double x, final double y, final int delta, final Player player) {}

        private boolean hasNonBlankText() {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (text[y][x] != ' ') {
                        return true;
                    }
                }
            }
            return false;
        }

        private String dump() {
            final StringBuilder builder = new StringBuilder();
            for (int y = 0; y < height; y++) {
                if (y > 0) {
                    builder.append('\n');
                }
                for (int x = 0; x < width; x++) {
                    builder.appendCodePoint(text[y][x]);
                }
            }
            return builder.toString();
        }

        private void put(final int column, final int row, final int value) {
            if (isInside(column, row)) {
                text[row][column] = value;
            }
        }

        private boolean isInside(final int column, final int row) {
            return column >= 0 && row >= 0 && column < width && row < height;
        }

        private static int[][] newText(final int width, final int height) {
            final int[][] value = new int[Math.max(1, height)][Math.max(1, width)];
            for (int y = 0; y < value.length; y++) {
                Arrays.fill(value[y], ' ');
            }
            return value;
        }
    }

    private static Object defaultValue(final Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private static Callback callback(final String name) {
        try {
            return LuaArchitectureTest.class.getDeclaredMethod(name).getAnnotation(Callback.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Callback(doc = "function():string -- Regular callback.")
    private static void labelCallback() {
    }

    @Callback(direct = true, doc = "function():string -- Direct callback.")
    private static void directCallback() {
    }

    @Callback(getter = true, setter = true)
    private static void accessorCallback() {
    }

    private static final class TestValue implements Value {
        private boolean unapplied;
        private String unapplyArgument;
        private String unapplyValue;
        private boolean disposed;

        @Override
        public Object apply(final Context context, final Arguments arguments) {
            return "applied:" + arguments.checkString(0);
        }

        @Override
        public void unapply(final Context context, final Arguments arguments) {
            unapplied = true;
            unapplyArgument = arguments.checkString(0);
            unapplyValue = arguments.count() > 1 ? arguments.checkString(1) : null;
        }

        @Override
        public Object[] call(final Context context, final Arguments arguments) {
            return new Object[]{"called:" + arguments.checkString(0)};
        }

        @Override
        public void dispose(final Context context) {
            disposed = true;
        }

        @Override
        public void load(final CompoundTag tag) {
        }

        @Override
        public void save(final CompoundTag tag) {
        }

        @Override
        public String toString() {
            return "test-value";
        }
    }

    private static final class TestMutableProcessor implements MutableProcessor {
        private Class<? extends Architecture> architecture;

        private TestMutableProcessor(final Class<? extends Architecture> architecture) {
            this.architecture = architecture;
        }

        @Override
        public Collection<Class<? extends Architecture>> allArchitectures() {
            return List.of(FirstArchitecture.class, SecondArchitecture.class);
        }

        @Override
        public void setArchitecture(final ItemStack stack, final Class<? extends Architecture> architecture) {
            this.architecture = architecture;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 8;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return architecture;
        }

        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }
    }

    private record TestMemoryDriver(double amount) implements Memory {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return Slot.Memory;
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }

        @Override
        public double amount(final ItemStack stack) {
            return amount;
        }
    }

    private record TestSignal(String name, Object[] args) implements Signal {
    }

    private static final class TestDeviceEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
        private final Map<String, String> deviceInfo;

        private TestDeviceEnvironment(final String name, final Map<String, String> deviceInfo) {
            this.deviceInfo = deviceInfo;
            setNode(Network.newNode(this, Visibility.Network).withComponent(name, Visibility.Network).create());
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }
    }

    @Architecture.Name("first")
    private abstract static class FirstArchitecture implements Architecture {
    }

    @Architecture.Name("second")
    private abstract static class SecondArchitecture implements Architecture {
    }
}
