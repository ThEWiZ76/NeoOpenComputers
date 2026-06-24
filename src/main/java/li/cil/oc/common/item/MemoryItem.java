package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.PassiveDeviceInfoEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class MemoryItem extends Item implements Memory {
    private final int tier;

    public MemoryItem(final Properties properties) {
        this(properties, 0);
    }

    public MemoryItem(final Properties properties, final int tier) {
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
        return Slot.Memory;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return new CompoundTag();
    }

    @Override
    public double amount(final ItemStack stack) {
        return switch (tier) {
            case 0 -> 192;
            case 1 -> 384;
            default -> 768;
        };
    }

    static ManagedEnvironment createDeviceInfoEnvironment(final int tier) {
        return new PassiveDeviceInfoEnvironment(deviceInfo(tier));
    }

    static Map<String, String> deviceInfo(final int tier) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Memory,
            DeviceInfo.DeviceAttribute.Description, "Memory bank",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Multipurpose RAM Type",
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
}
