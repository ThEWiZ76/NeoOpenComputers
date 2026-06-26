package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.PassiveDeviceInfoEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Map;

public class CpuItem extends Item implements MutableProcessor, CallBudget {
    private static final String ARCH_CLASS_TAG = "oc:archClass";
    private static final String ARCH_NAME_TAG = "oc:archName";
    private final int tier;

    public CpuItem(final Properties properties) {
        this(properties, 0);
    }

    public CpuItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.max(0, Math.min(2, tier));
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return createDeviceInfoEnvironment(tier(stack));
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.CPU;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }

    @Override
    public int supportedComponents(final ItemStack stack) {
        return ModSettings.cpuComponentCount(tier);
    }

    @Override
    public Class<? extends Architecture> architecture(final ItemStack stack) {
        final CompoundTag data = dataTag(stack);
        final String architectureClass = data.getString(ARCH_CLASS_TAG);
        if (!architectureClass.isEmpty()) {
            try {
                return Class.forName(architectureClass).asSubclass(Architecture.class);
            } catch (ClassNotFoundException | ClassCastException ignored) {
                data.remove(ARCH_CLASS_TAG);
                data.remove(ARCH_NAME_TAG);
            }
        }
        return li.cil.oc.api.Machine.architectures().stream()
            .findFirst()
            .orElse(li.cil.oc.api.Machine.LuaArchitecture);
    }

    @Override
    public Collection<Class<? extends Architecture>> allArchitectures() {
        return li.cil.oc.api.Machine.architectures();
    }

    @Override
    public void setArchitecture(final ItemStack stack, final Class<? extends Architecture> architecture) {
        if (!worksWith(stack)) {
            throw new IllegalArgumentException("Unsupported processor type.");
        }
        final CompoundTag data = dataTag(stack);
        data.putString(ARCH_CLASS_TAG, architecture.getName());
        final String architectureName = li.cil.oc.api.Machine.getArchitectureName(architecture);
        data.putString(ARCH_NAME_TAG, architectureName == null ? architecture.getSimpleName() : architectureName);
    }

    @Override
    public double getCallBudget(final ItemStack stack) {
        return callBudget(tier(stack));
    }

    static ManagedEnvironment createDeviceInfoEnvironment(final int tier) {
        return new PassiveDeviceInfoEnvironment(deviceInfo(tier));
    }

    static Map<String, String> deviceInfo(final int tier) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
            DeviceInfo.DeviceAttribute.Description, "CPU",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "FlexiArch " + (clampedTier + 1) + " Processor",
            DeviceInfo.DeviceAttribute.Clock, clock(clampedTier)
        );
    }

    private static String clock(final int tier) {
        return switch (tier) {
            case 0 -> "500";
            case 1 -> "1000";
            default -> "1500";
        };
    }

    static double callBudget(final int tier) {
        return ModSettings.callBudget(tier);
    }
}
