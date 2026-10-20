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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Standalone fixture: compiled against the API JAR, never OC implementation classes. */
@Mod("oc_api_smoke")
public final class ApiSmokeAddon {
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

    public static final class CardDriver extends li.cil.oc.api.prefab.DriverItem {
        @Override
        public boolean worksWith(ItemStack stack) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            return stack.is(Items.PAPER) && data != null && data.copyTag().getBoolean("oc_api_smoke");
        }
        @Override
        public String slot(ItemStack stack) { return Slot.Card; }
        @Override
        public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) { return new Card(); }
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
