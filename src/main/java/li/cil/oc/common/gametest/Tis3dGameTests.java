package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class Tis3dGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("tis3d")) {
            if (Boolean.getBoolean("neoopencomputers.testTIS3D")) throw new IllegalStateException("Requested TIS-3D fixture is missing");
            return List.of();
        }
        return List.of(
            new TestFunction("tis3d", "tis3d_adapter_serial_contract", "neoopencomputers:empty", 200, 0, true, Loaded::serialContract),
            new TestFunction("tis3d", "tis3d_native_module_transport", "neoopencomputers:empty", 400, 0, true, Loaded::nativeTransport));
    }

    private static final class Loaded {
        static void nativeTransport(GameTestHelper helper) {
            final var casePos = new BlockPos(1, 1, 1);
            final var casingPos = new BlockPos(3, 1, 1);
            helper.setBlock(casePos, ModBlocks.COMPUTER_CASE_TIER1.get());
            helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ADAPTER.get());
            helper.setBlock(casingPos, li.cil.tis3d.common.block.Blocks.CASING.get());
            helper.setBlock(new BlockPos(3, 1, 2), li.cil.tis3d.common.block.Blocks.CONTROLLER.get());
            final li.cil.tis3d.common.block.entity.CasingBlockEntity casing = helper.getBlockEntity(casingPos);
            final var face = casing.toLocal(Direction.WEST);
            casing.setModule(face, new li.cil.tis3d.common.module.SerialPortModule(casing, face));
            final var probe = new Probe(casing, casing.toLocal(Direction.UP));
            casing.setModule(probe.getFace(), probe);
            helper.setBlock(new BlockPos(3, 2, 2), net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,
                    net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(casePos);
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu))
                .setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(computer.SLOT_CPU, cpu);
            computer.setItem(computer.SLOT_MEMORY_0, new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(computer.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
                local eeprom = component.proxy(component.list('eeprom')())
                local address
                repeat
                  address = component.list('serial_port')()
                  if not address then computer.pullSignal(0.05) end
                until address
                local port = component.proxy(address)
                assert(port.read() == nil)
                port.setReading(true)
                assert(port.write(65535))
                local value
                repeat
                  value = port.read()
                  if value == nil then computer.pullSignal(0.05) end
                until value ~= nil
                assert(value == -32768, 'serial input sign or value lost')
                eeprom.setData('tis_ok')
                while true do computer.pullSignal() end
                """));
            helper.assertTrue(computer.toggleMachine(), "TIS native computer did not start");
            helper.startSequence()
                .thenWaitUntil(() -> {
                    final var marker = new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(computer.SLOT_EEPROM))
                        .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
                    helper.assertTrue(marker.equals("tis_ok") && computer.machine().isRunning(), "Native serial exchange incomplete: " + computer.machine().lastError());
                    helper.assertTrue(probe.received.equals(List.of((short) -1)) && probe.sent, "Real TIS pipes lost or duplicated output");
                })
                .thenExecute(() -> {
                    casing.setModule(probe.getFace(), null); casing.setModule(face, null);
                    computer.machine().stop();
                    helper.setBlock(new BlockPos(3, 2, 2), net.minecraft.world.level.block.Blocks.AIR);
                })
                .thenSucceed();
        }

        private static final class Probe extends li.cil.tis3d.api.prefab.module.AbstractModule {
            final java.util.ArrayList<Short> received = new java.util.ArrayList<>();
            boolean sent;

            Probe(li.cil.tis3d.api.machine.Casing casing, li.cil.tis3d.api.machine.Face face) { super(casing, face); }

            @Override public void step() {
                for (final var port : li.cil.tis3d.api.machine.Port.VALUES) {
                    final var input = getCasing().getReceivingPipe(getFace(), port);
                    if (!input.isReading()) input.beginRead();
                    if (input.canTransfer()) received.add(input.read());
                    final var output = getCasing().getSendingPipe(getFace(), port);
                    if (!sent && !output.isWriting()) output.beginWrite(Short.MIN_VALUE);
                }
            }

            @Override public void onWriteComplete(li.cil.tis3d.api.machine.Port port) {
                sent = true;
                for (final var other : li.cil.tis3d.api.machine.Port.VALUES) getCasing().getSendingPipe(getFace(), other).cancelWrite();
            }
        }

        static void serialContract(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ADAPTER.get());
            final var provider = li.cil.tis3d.common.provider.SerialInterfaceProviders.getProviderFor(helper.getLevel(), pos, Direction.NORTH).orElse(null);
            helper.assertTrue(provider != null, "No TIS-3D serial provider for OC adapter");
            final var serial = provider.getInterface(helper.getLevel(), pos, Direction.NORTH).orElseThrow();
            final var component = (Component) ((Environment) serial).node();
            try {
                helper.assertTrue(component.name().equals("serial_port") && component.methods().containsAll(List.of("setReading", "read", "write")), "Missing serial callbacks");
                helper.assertTrue(!serial.canWrite() && !serial.canRead(), "New serial port must not read or contain output");
                helper.assertTrue(component.invoke("read", null) == null, "Empty read should return no values");
                helper.assertTrue(component.invoke("setReading", null, true) == null, "setReading return contract changed");
                for (int i = 0; i < 128; i++) {
                    helper.assertTrue(serial.canWrite(), "Input buffer filled too early"); serial.write((short) (32768 + i));
                    helper.assertTrue(Boolean.TRUE.equals(component.invoke("write", null, 65535 + i)[0]), "Output buffer filled too early");
                }
                helper.assertTrue(!serial.canWrite(), "Input buffer exceeds128 values");
                final var full = component.invoke("write", null, 123);
                helper.assertTrue(Boolean.FALSE.equals(full[0]) && full[1].equals("buffer full"), "Missing full-buffer error");
                helper.assertTrue(serial.canRead(), "Lua output unavailable to serial module");
                final var adapter = (Environment) helper.getLevel().getBlockEntity(pos);
                helper.assertTrue(adapter.node().network() == component.network(), "Serial component did not join adapter network");
                final var saved = new CompoundTag(); serial.save(saved);
                serial.reset();
                final var restored = provider.getInterface(helper.getLevel(), pos, Direction.NORTH).orElseThrow();
                restored.load(saved);
                try {
                    final var reloaded = (Component) ((Environment) restored).node();
                    helper.assertTrue(reloaded.address().equals(saved.getString("address")), "Saved serial address lost");
                    for (int i = 0; i < 128; i++) {
                        helper.assertTrue(((Number) reloaded.invoke("read", null)[0]).intValue() == (short) (32768 + i), "Signed input/order lost on load");
                        helper.assertTrue(restored.canRead() && restored.peek() == (short) (65535 + i), "Signed output/order lost on load");
                        helper.assertTrue(restored.peek() == (short) (65535 + i), "Peek consumed output"); restored.skip();
                    }
                    helper.assertTrue(!restored.canRead() && restored.canWrite(), "Restored queues failed to drain");
                    helper.assertTrue(provider.stillValid(helper.getLevel(), pos, Direction.NORTH, restored), "Valid adapter interface was invalidated");
                    helper.setBlock(new BlockPos(1, 1, 1), net.minecraft.world.level.block.Blocks.STONE);
                    helper.assertTrue(!provider.stillValid(helper.getLevel(), pos, Direction.NORTH, restored), "Removed adapter retained serial interface");
                } finally { restored.reset(); }
            } catch (Exception failure) { throw new IllegalStateException(failure); }
            finally { serial.reset(); }
            helper.succeed();
        }
    }
}
