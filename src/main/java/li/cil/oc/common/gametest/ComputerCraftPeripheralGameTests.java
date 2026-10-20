package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("neoopencomputers")
public final class ComputerCraftPeripheralGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) return List.of();
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(test("cc_peripheral_native_printer", Loaded::printer), test("cc_peripheral_native_inventory", Loaded::inventory),
                test("cc_peripheral_codec_tasks_lifecycle", Loaded::codecAndTasks), test("cc_peripheral_mount_cleanup", Loaded::mounts),
                test("cc_peripheral_blacklist_and_native_priority", Loaded::blacklists), test("cc_peripheral_replacement", Loaded::replacement));
        }
        static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> body) {
            return new TestFunction("ccPeripherals", name, "neoopencomputers:ae2_export", 600, 0, true, body);
        }
        static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("computercraft", name); }
        static AdapterBlockEntity adapter(GameTestHelper helper, BlockPos pos) {
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ADAPTER.get().defaultBlockState());
            final var adapter = (AdapterBlockEntity) helper.getLevel().getBlockEntity(pos);
            adapter.onLoad(); adapter.refreshNeighbors(); return adapter;
        }
        static Component component(GameTestHelper helper, AdapterBlockEntity adapter, String method) {
            for (final var node : adapter.node().reachableNodes()) if (node instanceof Component value && value.methods().contains(method)) return value;
            helper.fail("OC adapter is missing ComputerCraft peripheral method " + method); return null;
        }
        static ComputerCaseBlockEntity boot(GameTestHelper helper, BlockPos pos, String program) {
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            final var computer = (ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get())); computer.setItem(4, cpu);
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom(program));
            computer.onLoad(); Network.joinOrCreateNetwork(computer);
            ((Connector) computer.node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Native peripheral computer did not start"); return computer;
        }
        static boolean marked(ComputerCaseBlockEntity computer, String value) {
            return new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8).equals(value);
        }

        static void printer(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(4, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.get(id("printer")).defaultBlockState());
            final var printer = (dan200.computercraft.shared.peripheral.printer.PrinterBlockEntity) helper.getLevel().getBlockEntity(pos);
            printer.setItem(0, new ItemStack(Items.BLACK_DYE, 2)); printer.setItem(1, new ItemStack(Items.PAPER, 2));
            final var adapter = adapter(helper, pos.west()); final var component = component(helper, adapter, "newPage");
            final var context = new TestContext(101); context.node().connect(adapter.node());
            final var background = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { component.invoke("newPage", context); throw new AssertionError("Off-thread peripheral action was not deferred"); }
                catch (li.cil.oc.api.machine.LimitReachedException expected) { }
                catch (Exception exception) { throw new RuntimeException(exception); }
            });
            final var computer = new ComputerCaseBlockEntity[1];
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(background.isDone(), "Waiting for direct-call thread guard"))
                .thenExecute(() -> {
                    background.join();
                    helper.assertTrue(printer.getItem(0).getCount() == 2 && printer.getItem(1).getCount() == 2, "Off-thread call consumed printer supplies");
                    context.node().remove();
                    computer[0] = boot(helper, pos.west(2), """
                        local p = component.proxy(assert(component.list('printer')(), 'printer missing'))
                        assert(p.getInkLevel() == 2 and p.getPaperLevel() == 2)
                        assert(p.newPage())
                        local w,h = p.getPageSize(); assert(w == 25 and h == 21)
                        p.setCursorPos(2,2)
                        local x,y = p.getCursorPos(); assert(x == 2 and y == 2)
                        p.setCursorPos(1,1)
                        p.setPageTitle(); p.setPageTitle('OC bridge')
                        p.write('OC printer')
                        assert(p.endPage())
                        component.proxy(component.list('eeprom')()).setData('printer-verified')
                        while true do computer.pullSignal() end
                        """);
                }).thenWaitUntil(() -> helper.assertTrue(marked(computer[0], "printer-verified"), "Native printer program failed: " + computer[0].machine().lastError()))
                .thenExecute(() -> {
                    final var page = printer.getItem(7);
                    final var data = dan200.computercraft.shared.media.items.PrintoutData.getOrEmpty(page);
                    helper.assertTrue(page.is(BuiltInRegistries.ITEM.get(id("printed_page"))) && page.getCount() == 1
                        && data.title().equals("OC bridge") && data.lines().getFirst().text().startsWith("OC printer")
                        && printer.getItem(0).getCount() == 1 && printer.getItem(1).getCount() == 1, "Native CC output or exact printer supply accounting is wrong");
                    computer[0].machine().stop();
                }).thenSucceed();
        }

        static void inventory(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
            helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.CHEST.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pos.west(), Blocks.CHEST.defaultBlockState());
            final var source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(pos.east());
            final var target = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(pos.west());
            source.setItem(0, new ItemStack(Items.DIAMOND, 5));
            final var adapter = adapter(helper, pos); component(helper, adapter, "pushItems");
            final var computer = boot(helper, pos.south(), """
                local source,target
                for address in component.list() do
                  local methods = component.methods(address)
                  if methods and methods.pushItems and methods.getItemDetail then
                    local p = component.proxy(address)
                    local size = p.size(); assert(size == 27, 'inventory size: '..tostring(size)..' type '..tostring(p.type))
                    if p.getItemDetail(1) then source = p else target = p end
                  end
                end
                assert(source and target, 'inventory peripherals missing')
                assert(source.getItemDetail(1).name == 'minecraft:diamond')
                assert(source.pushItems(target.address,1,3,2) == 3)
                assert(source.getItemDetail(1).count == 2 and target.getItemDetail(2).count == 3)
                component.proxy(component.list('eeprom')()).setData('inventory-verified')
                while true do computer.pullSignal() end
                """);
            helper.succeedWhen(() -> {
                helper.assertTrue(marked(computer, "inventory-verified"), "Native generic inventory program failed: " + computer.machine().lastError());
                helper.assertTrue(source.getItem(0).is(Items.DIAMOND) && source.getItem(0).getCount() == 2
                    && target.getItem(1).is(Items.DIAMOND) && target.getItem(1).getCount() == 3, "Native CC transfer lost/duplicated items or ignored one-based target slot");
                computer.machine().stop();
            });
        }

        static void codecAndTasks(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(4, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState());
            final var probe = new Probe(helper.getLevel(), null);
            try {
                final var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, probe);
                final var adapter = adapter(helper, pos.west()); final var component = component(helper, adapter, "echo");
                final var first = new TestContext(201); final var second = new TestContext(202);
                first.node().connect(adapter.node()); second.node().connect(adapter.node());
                helper.assertTrue(probe.attached == 2, "Peripheral was not attached exactly once per OC context");
                final var echoed = component.invoke("echo", first, "Euro €".getBytes(StandardCharsets.UTF_8), new byte[]{0, (byte) 128, (byte) 255});
                helper.assertTrue(echoed[0].equals("Euro €") && Arrays.equals((byte[]) echoed[1], new byte[]{0, (byte) 128, (byte) 255}), "CC text/binary argument or ByteBuffer result conversion is wrong");
                final var table = new LinkedHashMap<String, Object>(); table.put("text", "Euro €".getBytes(StandardCharsets.UTF_8)); table.put("self", table);
                final var nested = component.invoke("nested", first, table, new byte[]{0, (byte) 128, (byte) 255});
                helper.assertTrue(nested[0] instanceof Map<?, ?> converted && converted.get("text").equals("Euro €") && converted.get("self") == converted
                    && Arrays.equals((byte[]) nested[1], new byte[]{0, (byte) 128, (byte) 255}), "Recursive tables, cycles or dropped binary argument were corrupted");
                final var identity = component.invoke("identity", first);
                helper.assertTrue(((Number) identity[0]).intValue() == first.node().address().hashCode() && identity[1] instanceof String && Boolean.TRUE.equals(identity[2]), "Wrong IComputerAccess identity/work monitor");
                component.invoke("signal", second, "Euro €".getBytes(StandardCharsets.UTF_8));
                helper.assertTrue(first.signals.isEmpty() && second.signals.size() == 1 && second.signals.getFirst().name.equals("probe")
                    && second.signals.getFirst().args[0].equals("Euro €"), "Peripheral event crossed contexts or lost text");
                expectError(helper, () -> component.invoke("yield", first), "unsupported");
                final long taskId = ((Number) component.invoke("async", first, "job")[0]).longValue();
                helper.assertTrue(probe.executions == 0, "Asynchronous task ran inside the peripheral call");
                helper.runAtTickTime(3, () -> {
                    helper.assertTrue(probe.executions == 1 && first.signals.stream().anyMatch(signal -> signal.name.equals("task_complete")
                        && ((Number) signal.args[0]).longValue() == taskId && Boolean.TRUE.equals(signal.args[1])), "Queued CC task did not complete exactly once on its owner");
                    try { component.invoke("async", first, "cancelled"); } catch (Exception exception) { throw new RuntimeException(exception); }
                    first.node().remove();
                    helper.assertTrue(probe.detached == 1, "First context detach was missed or duplicated");
                });
                helper.runAtTickTime(6, () -> {
                    helper.assertTrue(probe.executions == 1, "Detached computer's queued work still executed");
                    second.node().remove(); helper.getLevel().removeBlock(adapter.getBlockPos(), false);
                    helper.assertTrue(probe.detached == 2, "Peripheral removal did not detach all contexts once");
                    try { fixture.close(); } catch (Exception exception) { throw new RuntimeException(exception); }
                    helper.succeed();
                });
            } catch (Exception exception) { throw new RuntimeException(exception); }
        }

        static void mounts(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(4, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState());
            final var mount = dan200.computercraft.api.ComputerCraftAPI.createSaveDirMount(helper.getLevel().getServer(), "oc-peripheral-fixture/" + pos.asLong(), 16384);
            final var probe = new Probe(helper.getLevel(), mount);
            final var context = new TestContext(301);
            try (var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, probe)) {
                final var adapter = adapter(helper, pos.west()); final var component = component(helper, adapter, "mount");
                context.node().connect(adapter.node());
                component.invoke("mount", context);
                final var filesystems = new ArrayList<Component>();
                for (var node : context.node().reachableNodes()) if (node instanceof Component value && value.name().equals("filesystem")) filesystems.add(value);
                helper.assertTrue(filesystems.size() == 2, "CC mounts did not create per-computer OC filesystems");
                final var readOnly = filesystems.stream().filter(value -> label(value, context).equals("fixture-ro")).findFirst().orElseThrow();
                final var writable = filesystems.stream().filter(value -> label(value, context).equals("fixture-rw")).findFirst().orElseThrow();
                helper.assertTrue(Boolean.TRUE.equals(readOnly.invoke("isReadOnly", context)[0]) && Boolean.FALSE.equals(writable.invoke("isReadOnly", context)[0]), "Readonly mount leaked WritableMount capability");
                expectError(helper, () -> readOnly.invoke("open", context, "forbidden", "wb"), "read");
                var handle = writable.invoke("open", context, "binary", "wb")[0];
                writable.invoke("write", context, handle, new byte[]{0, (byte) 128, (byte) 255});
                writable.invoke("seek", context, handle, "set", 1);
                final var savedWriter = new net.minecraft.nbt.CompoundTag(); ((ManagedEnvironment) writable.host()).save(savedWriter); ((ManagedEnvironment) writable.host()).load(savedWriter);
                writable.invoke("write", context, handle, new byte[]{42});
                writable.invoke("close", context, handle);
                handle = writable.invoke("open", context, "binary", "ab")[0]; writable.invoke("write", context, handle, new byte[]{7}); writable.invoke("close", context, handle);
                handle = writable.invoke("open", context, "binary", "rb")[0];
                helper.assertTrue(Arrays.equals((byte[]) writable.invoke("read", context, handle, 1)[0], new byte[]{0}), "CC mount lost first binary byte");
                final var saved = new net.minecraft.nbt.CompoundTag(); ((ManagedEnvironment) writable.host()).save(saved); ((ManagedEnvironment) writable.host()).load(saved);
                helper.assertTrue(Arrays.equals((byte[]) writable.invoke("read", context, handle, 3)[0], new byte[]{42, (byte) 255, 7}), "Mount handle restore truncated data or lost position");
                helper.assertTrue(((Number) writable.invoke("spaceTotal", context)[0]).longValue() == mount.getCapacity(), "Writable mount capacity mismatch");
                expectError(helper, () -> writable.invoke("open", context, "../escape", "wb"), "escape");
                probe.failDetach = true;
                context.node().remove();
                helper.assertTrue(probe.detached == 1, "Unmount owner did not detach");
                mount.delete("binary");
                helper.assertTrue(!mount.exists("binary") && !mount.exists("forbidden"), "Mount handles leaked or readonly call created a file");
                helper.getLevel().removeBlock(adapter.getBlockPos(), false);
            } catch (Exception exception) { throw new RuntimeException(exception); }
            finally { context.node().remove(); }
            helper.succeed();
        }

        static String label(Component fs, Context context) {
            try { return (String) fs.invoke("getLabel", context)[0]; } catch (Exception exception) { throw new RuntimeException(exception); }
        }

        static void replacement(GameTestHelper helper) {
            final var world = helper.getLevel(); final var pos = helper.absolutePos(new BlockPos(4, 2, 2));
            final var driver = new li.cil.oc.common.integration.computercraft.PeripheralDriver();
            final var unloaded = pos.offset(100000, 0, 100000);
            helper.assertTrue(!world.hasChunkAt(unloaded), "Unloaded lookup fixture already loaded");
            helper.assertTrue(!driver.worksWith(world, unloaded, Direction.EAST) && driver.createEnvironment(world, unloaded, Direction.EAST) == null
                && !world.hasChunkAt(unloaded), "CC lookup loaded an absent chunk");
            world.setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState());
            final var old = new Probe(world, null); final var replacement = new Probe(world, null); final var context = new TestContext(401);
            try {
                final var firstFixture = TestComputerCraftCapabilities.attach(world, pos, Direction.WEST, old);
                final var adapter = adapter(helper, pos.west()); final var original = component(helper, adapter, "echo");
                context.node().connect(adapter.node());
                helper.assertTrue(old.attached == 1, "Original peripheral attach missing");
                firstFixture.close();
                final var nextFixture = TestComputerCraftCapabilities.attach(world, pos, Direction.WEST, replacement);
                expectError(helper, () -> original.invoke("echo", context, "stale", "stale"), "unavailable");
                helper.runAtTickTime(3, () -> {
                    final var current = component(helper, adapter, "echo");
                    helper.assertTrue(current != original && old.detached == 1 && replacement.attached == 1, "Peripheral replacement did not refresh/reattach exactly once");
                    context.node().remove(); world.removeBlock(adapter.getBlockPos(), false);
                    helper.assertTrue(replacement.detached == 1, "Replacement context cleanup missing");
                    try { nextFixture.close(); } catch (Exception exception) { throw new RuntimeException(exception); }
                    helper.succeed();
                });
            } catch (Exception exception) { context.node().remove(); throw new RuntimeException(exception); }
        }
        interface Call { void run() throws Exception; }
        static void expectError(GameTestHelper helper, Call call, String fragment) {
            try { call.run(); helper.fail("Expected error containing " + fragment); }
            catch (Exception exception) { helper.assertTrue(exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains(fragment), "Wrong peripheral error: " + exception); }
        }

        @SuppressWarnings("unchecked")
        static void blacklists(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(4, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState());
            final var probe = new ConfigBlockedProbe(helper.getLevel());
            final net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<List<String>> config = ModSettings.SPEC.getValues().get("integration.peripheralBlacklist");
            final var previous = List.copyOf(config.get());
            try {
                try (var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, probe)) {
                    helper.assertTrue(hasMethod(helper, pos, "echo"), "Baseline CC probe unavailable");
                    config.set(List.of(ConfigBlockedProbe.class.getName()));
                    helper.assertTrue(!hasMethod(helper, pos, "echo"), "Configured peripheral class blacklist was ignored");
                }
                try (var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, new AllowedProbe(helper.getLevel()))) {
                    helper.assertTrue(hasMethod(helper, pos, "echo"), "Object-level blacklist override did not beat class policy");
                }
                config.set(previous);
                helper.getLevel().setBlockAndUpdate(pos, Blocks.COMMAND_BLOCK.defaultBlockState());
                try (var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, probe)) {
                    helper.assertTrue(!hasMethod(helper, pos, "echo"), "Default command-block blacklist was bypassed");
                    config.set(List.of("net.minecraft.tileentity.TileEntityCommandBlock"));
                    helper.assertTrue(!hasMethod(helper, pos, "echo"), "Legacy command-block blacklist name was not migrated");
                }
                config.set(List.of());
                helper.getLevel().setBlockAndUpdate(pos, ModBlocks.SCREEN_TIER1.get().defaultBlockState());
                try (var fixture = TestComputerCraftCapabilities.attach(helper.getLevel(), pos, Direction.WEST, probe)) {
                    helper.assertTrue(!hasMethod(helper, pos, "echo"), "Native OC environment received a duplicate CC component");
                }
            } catch (Exception exception) { throw new RuntimeException(exception); }
            finally { config.set(previous); }
            helper.succeed();
        }
        static boolean hasMethod(GameTestHelper helper, BlockPos pos, String method) {
            final var driver = Driver.driverFor(helper.getLevel(), pos, Direction.EAST);
            final var env = driver == null ? null : driver.createEnvironment(helper.getLevel(), pos, Direction.EAST);
            try { return env != null && env.node() instanceof Component component && component.methods().contains(method); }
            finally { if (env != null && env.node() != null) env.node().remove(); }
        }

        record Signal(String name, Object[] args) { }
        static final class TestContext extends AbstractManagedEnvironment implements Context {
            final List<Signal> signals = new ArrayList<>();
            TestContext(int id) {
                setNode(Network.newNode(this, Visibility.Network).withConnector(10000).create());
                final var data = new net.minecraft.nbt.CompoundTag(); data.putString("address", String.format(Locale.ROOT, "00000000-0000-0000-0000-%012d", id)); node().load(data);
                Network.joinNewNetwork(node()); ((Connector) node()).changeBuffer(10000);
            }
            public boolean canInteract(String player) { return true; }
            public boolean isRunning() { return true; }
            public boolean isPaused() { return false; }
            public boolean start() { return false; }
            public boolean stop() { return false; }
            public boolean pause(double seconds) { return false; }
            public void consumeCallBudget(double cost) { }
            public boolean signal(String name, Object... args) { signals.add(new Signal(name, args)); return true; }
        }

        static class Probe implements dan200.computercraft.api.peripheral.IDynamicPeripheral {
            final net.minecraft.server.level.ServerLevel world;
            final dan200.computercraft.api.filesystem.WritableMount mount;
            int attached, detached, executions;
            boolean failDetach;
            Probe(net.minecraft.server.level.ServerLevel world, dan200.computercraft.api.filesystem.WritableMount mount) { this.world = world; this.mount = mount; }
            public String getType() { return "oc_test_cc"; }
            public String[] getMethodNames() { return new String[]{"echo", "identity", "signal", "async", "mount", "yield", "nested"}; }
            public boolean equals(dan200.computercraft.api.peripheral.IPeripheral other) { return other == this; }
            public void attach(dan200.computercraft.api.peripheral.IComputerAccess computer) { attached++; }
            public void detach(dan200.computercraft.api.peripheral.IComputerAccess computer) { detached++; if (failDetach) throw new IllegalStateException("deliberate detach failure"); }
            public dan200.computercraft.api.lua.MethodResult callMethod(dan200.computercraft.api.peripheral.IComputerAccess computer, dan200.computercraft.api.lua.ILuaContext context, int method, dan200.computercraft.api.lua.IArguments args) throws dan200.computercraft.api.lua.LuaException {
                if (!world.getServer().isSameThread()) throw new dan200.computercraft.api.lua.LuaException("Peripheral ran outside server thread");
                return switch (getMethodNames()[method]) {
                    case "echo" -> dan200.computercraft.api.lua.MethodResult.of(args.getString(0), args.getBytes(1));
                    case "nested" -> dan200.computercraft.api.lua.MethodResult.of(args.getTable(0), args.drop(1).getBytes(0));
                    case "identity" -> dan200.computercraft.api.lua.MethodResult.of(computer.getID(), computer.getAttachmentName(), computer.getMainThreadMonitor().canWork());
                    case "signal" -> { computer.queueEvent("probe", args.getString(0)); yield dan200.computercraft.api.lua.MethodResult.of(); }
                    case "async" -> dan200.computercraft.api.lua.MethodResult.of(context.issueMainThreadTask(() -> { executions++; return new Object[]{args.getString(0)}; }));
                    case "mount" -> { computer.mount("ro", mount, "fixture-ro"); computer.mountWritable("rw", mount, "fixture-rw"); yield dan200.computercraft.api.lua.MethodResult.of(); }
                    default -> dan200.computercraft.api.lua.MethodResult.pullEvent("unsupported", ignored -> dan200.computercraft.api.lua.MethodResult.of());
                };
            }
        }
        static class ConfigBlockedProbe extends Probe { ConfigBlockedProbe(net.minecraft.server.level.ServerLevel world) { super(world, null); } }
        static final class AllowedProbe extends ConfigBlockedProbe implements li.cil.oc.api.network.BlacklistedPeripheral {
            AllowedProbe(net.minecraft.server.level.ServerLevel world) { super(world); }
            public boolean isPeripheralBlacklisted() { return false; }
        }
    }
}
