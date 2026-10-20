package example.ocapi;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.api.util.Lifecycle;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Standalone fixture: compiled against the API JAR, never OC implementation classes. */
@Mod("oc_api_smoke")
public final class ApiSmokeAddon {
    private static final Map<String, List<TrackedEnvironment>> TRACKED = new LinkedHashMap<>();
    public ApiSmokeAddon(IEventBus bus) {
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            Driver.add(new CardDriver());
            Driver.add((value, output) -> {
                if (value instanceof Description description) {
                    output.put("name", description.name());
                    output.put("bytes", new byte[]{0, (byte) 255, 42});
                }
            });
        }));
    }

    private record Description(String name) { }

    public static final class CardDriver extends li.cil.oc.api.prefab.DriverItem implements li.cil.oc.api.driver.item.Chargeable {
        @Override
        public boolean worksWith(ItemStack stack) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            return stack.is(Items.PAPER) && data != null && data.copyTag().getBoolean("oc_api_smoke");
        }
        @Override
        public String slot(ItemStack stack) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().getBoolean("oc_api_smoke_tablet")) return Slot.Tablet;
            return data != null && data.copyTag().getBoolean("oc_api_smoke_upgrade") ? Slot.Upgrade : Slot.Card;
        }
        @Override
        @SuppressWarnings("deprecation")
        public CompoundTag dataTag(ItemStack stack) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().getBoolean("oc_api_smoke_null_tag")) return null;
            if (data != null && data.copyTag().getBoolean("oc_api_smoke_custom_tag")) {
                final var root = data.getUnsafe();
                if (!root.contains("apiSmokeData")) root.put("apiSmokeData", new CompoundTag());
                return root.getCompound("apiSmokeData");
            }
            return super.dataTag(stack);
        }
        @Override public boolean canCharge(ItemStack stack) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && data.copyTag().getBoolean("oc_api_smoke_tablet");
        }
        @Override public double charge(ItemStack stack, double amount, boolean simulate) {
            if (!canCharge(stack) || !(amount > 0)) return 0;
            final var root = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            final double accepted = Math.min(amount, Math.max(0, 1000 - root.getDouble("apiSmokeCharge")));
            if (!simulate && accepted > 0) {
                root.putDouble("apiSmokeCharge", root.getDouble("apiSmokeCharge") + accepted);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            }
            return accepted;
        }
        @Override
        public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
            final var data = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            if (data.contains("oc_api_smoke_lifecycle_id")) return new TrackedEnvironment(data.getString("oc_api_smoke_lifecycle_id"), data.getBoolean("oc_api_smoke_no_node"));
            return new Card();
        }
    }

    public static Map<String, Object> observe(String id) {
        final var entries = TRACKED.get(id);
        if (entries == null || entries.isEmpty()) return Map.of("phase", "missing", "trace", List.of());
        return entries.getLast().observation();
    }

    public static List<Map<String, Object>> history(String id) {
        return TRACKED.getOrDefault(id, List.of()).stream().map(TrackedEnvironment::observation).toList();
    }

    public static void setTrackedValue(String id, int value) { TRACKED.get(id).getLast().value = value; }

    public static final class TrackedEnvironment extends AbstractManagedEnvironment implements Lifecycle {
        private final List<String> trace = new ArrayList<>();
        private LifecycleState phase;
        private boolean loaded, updated, updatedOutsideLifetime;
        private int value;

        TrackedEnvironment(String id, boolean noNode) {
            if (!noNode) setNode(Network.newNode(this, Visibility.Network).withComponent("api_lifecycle").create());
            TRACKED.computeIfAbsent(id, ignored -> new ArrayList<>()).add(this);
        }

        Map<String, Object> observation() {
            return Map.of("phase", phase == null ? "missing" : phase.name(), "trace", List.copyOf(trace),
                "loaded", loaded, "updated", updated, "updatedOutsideLifetime", updatedOutsideLifetime, "value", value);
        }

        @Override public void onLifecycleStateChange(LifecycleState state) { phase = state; trace.add(state.name()); }
        @Override public boolean canUpdate() { return true; }
        @Override public void update() { updated = true; if (phase != LifecycleState.Initialized) updatedOutsideLifetime = true; }
        @Override public void load(CompoundTag data) { trace.add("load"); super.load(data); value = data.getInt("trackedValue"); loaded = true; }
        @Override public void save(CompoundTag data) { super.save(data); data.putInt("trackedValue", value); }
        @Callback public Object[] getPhase(Context context, Arguments args) { return new Object[]{phase == null ? "missing" : phase.name()}; }
        @Callback public Object[] getValue(Context context, Arguments args) { return new Object[]{value}; }
        @Callback public Object[] setValue(Context context, Arguments args) { value = args.checkInteger(0); return new Object[]{value}; }
    }

    public static final class Card extends AbstractManagedEnvironment {
        private int count;
        public Card() { setNode(Network.newNode(this, Visibility.Network).withComponent("api_smoke").create()); }

        @Callback
        public Object[] increment(Context context, Arguments args) { count += args.checkInteger(0); return new Object[]{count}; }
        @Callback(direct = true)
        public Object[] getCount(Context context, Arguments args) { return new Object[]{count}; }
        @Callback
        public Object[] describe(Context context, Arguments args) { return new Object[]{new Description(args.checkString(0))}; }
        @Callback
        public Object[] counter(Context context, Arguments args) { return new Object[]{new Counter(args.checkInteger(0))}; }

        @Override
        public void load(CompoundTag data) { super.load(data); count = data.getInt("count"); }
        @Override
        public void save(CompoundTag data) { super.save(data); data.putInt("count", count); }
    }

    public static final class Counter extends AbstractValue {
        private int count;
        public Counter() { }
        public Counter(int count) { this.count = count; }
        @Callback
        public Object[] add(Context context, Arguments args) { count += args.checkInteger(0); return new Object[]{count}; }
        @Override
        public void load(CompoundTag data) { count = data.getInt("count"); }
        @Override
        public void save(CompoundTag data) { data.putInt("count", count); }
    }
}
