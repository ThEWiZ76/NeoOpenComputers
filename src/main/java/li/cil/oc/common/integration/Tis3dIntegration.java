package li.cil.oc.common.integration;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayDeque;
import java.util.Optional;

/** Optional TIS-3D types are resolved only when its mod is installed. */
public final class Tis3dIntegration {
    private Tis3dIntegration() { }

    public static void register(IEventBus bus) {
        if (ModList.get().isLoaded("tis3d")) Loaded.register(bus);
    }

    private static final class Loaded {
        static void register(IEventBus bus) {
            final var providers = DeferredRegister.create(li.cil.tis3d.api.serial.SerialInterfaceProvider.REGISTRY, "neoopencomputers");
            providers.register("adapter", Provider::new);
            providers.register(bus);
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) Client.register(bus);
        }

        private static Adapter adapter(Level level, BlockPos pos) {
            return level != null && pos != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Adapter adapter
                ? adapter : null;
        }

        private static final class Provider implements li.cil.tis3d.api.serial.SerialInterfaceProvider {
            @Override public boolean matches(Level level, BlockPos pos, Direction side) { return adapter(level, pos) != null; }

            @Override public Optional<li.cil.tis3d.api.serial.SerialInterface> getInterface(Level level, BlockPos pos, Direction side) {
                final var adapter = adapter(level, pos);
                return adapter == null ? Optional.empty() : Optional.of(new SerialEnvironment(adapter));
            }

            @Override public boolean stillValid(Level level, BlockPos pos, Direction side, li.cil.tis3d.api.serial.SerialInterface serial) {
                return serial instanceof SerialEnvironment port && adapter(level, pos) == port.adapter;
            }

            @Override public Optional<li.cil.tis3d.api.serial.SerialProtocolDocumentationReference> getDocumentationReference() {
                return Optional.of(new li.cil.tis3d.api.serial.SerialProtocolDocumentationReference(
                    Component.literal("OpenComputers Adapter"), li.cil.manual.api.ManualModel.LANGUAGE_KEY + "/protocols/opencomputersadapter.md"));
            }
        }

        public static final class SerialEnvironment extends AbstractManagedEnvironment implements li.cil.tis3d.api.serial.SerialInterface {
            private static final int CAPACITY = 128;
            private final Adapter adapter;
            private final ArrayDeque<Short> input = new ArrayDeque<>();
            private final ArrayDeque<Short> output = new ArrayDeque<>();
            private boolean reading;

            SerialEnvironment(Adapter adapter) {
                this.adapter = adapter;
                setNode(Network.newNode(this, Visibility.Network).withComponent("serial_port", Visibility.Network).create());
            }

            @Callback(doc = "function(enabled:boolean) -- Enable or disable receiving values from the serial module.")
            public synchronized Object[] setReading(Context context, Arguments args) { reading = args.checkBoolean(0); return null; }

            @Callback(doc = "function():number -- Read the oldest received signed 16-bit value, or nil when empty.")
            public synchronized Object[] read(Context context, Arguments args) { return input.isEmpty() ? null : new Object[]{input.removeFirst()}; }

            @Callback(doc = "function(value:number):boolean[,string] -- Queue a 16-bit value for the serial module.")
            public synchronized Object[] write(Context context, Arguments args) {
                if (output.size() >= CAPACITY) return new Object[]{false, "buffer full"};
                output.addLast((short) args.checkInteger(0));
                return new Object[]{true};
            }

            @Override public synchronized boolean canWrite() { return reading && input.size() < CAPACITY; }
            @Override public synchronized void write(short value) { if (input.size() < CAPACITY) input.addLast(value); }
            @Override public synchronized boolean canRead() {
                if (adapter.node().network() != node().network() || node().network() == null) adapter.node().connect(node());
                return !output.isEmpty();
            }
            @Override public synchronized short peek() { return output.getFirst(); }
            @Override public synchronized void skip() { output.removeFirst(); }
            @Override public synchronized void reset() { input.clear(); output.clear(); node().remove(); }

            @Override public synchronized void save(CompoundTag tag) {
                // Keep the original flat node layout inside TIS's serialInterface tag.
                node().save(tag);
                tag.putIntArray("readBuffer", input.stream().mapToInt(Short::intValue).toArray());
                tag.putIntArray("writeBuffer", output.stream().mapToInt(Short::intValue).toArray());
                tag.putBoolean("isReading", reading);
            }

            @Override public synchronized void load(CompoundTag tag) {
                final var reads = tag.getIntArray("readBuffer"); final var writes = tag.getIntArray("writeBuffer");
                if (reads.length > CAPACITY || writes.length > CAPACITY) throw new IllegalArgumentException("serial buffer exceeds capacity");
                node().load(tag);
                input.clear(); output.clear();
                for (int value : reads) input.addLast((short) value);
                for (int value : writes) output.addLast((short) value);
                reading = tag.getBoolean("isReading");
            }
        }
    }

    private static final class Client {
        static void register(IEventBus bus) {
            final var documents = DeferredRegister.create(li.cil.manual.api.util.Constants.DOCUMENT_PROVIDER_REGISTRY, "neoopencomputers");
            documents.register("tis3d_protocol", () -> new li.cil.manual.api.prefab.provider.NamespaceDocumentProvider("neoopencomputers", "doc/tis3d"));
            documents.register(bus);
        }
    }
}
