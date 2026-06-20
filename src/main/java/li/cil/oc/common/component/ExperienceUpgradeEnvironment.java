package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Map;

public class ExperienceUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "experience";
    private static final String TAG_EXPERIENCE = "oc:xp";
    private static final int MAX_LEVEL = 30;
    private static final double BASE_XP_TO_LEVEL = 50D;
    private static final double CONSTANT_XP_GROWTH = 8D;
    private static final double EXPONENTIAL_XP_GROWTH = 2D;
    private static final double BUFFER_PER_LEVEL = 5_000D;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Knowledge database",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "ERSO (Event Recorder and Self-Optimizer)",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(MAX_LEVEL)
    );

    private final Agent host;
    private double experience;
    private int level;

    public ExperienceUpgradeEnvironment(final Agent host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector(0D).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public void addExperience(final double value) {
        if (value <= 0D || level >= MAX_LEVEL) {
            return;
        }
        experience = Math.max(0D, experience + value);
        updateXpInfo();
        host.markChanged();
    }

    @Callback(direct = true, doc = "function():number -- The current level of experience stored in this experience upgrade.")
    public Object[] level(final Context context, final Arguments arguments) {
        return new Object[]{calculateExperienceLevel(level, experience)};
    }

    @Callback(doc = "function():boolean -- Tries to consume the selected item and store its experience in this upgrade.")
    public Object[] consume(final Context context, final Arguments arguments) {
        if (level >= MAX_LEVEL) {
            return new Object[]{null, "max level"};
        }

        final Container inventory = host.mainInventory();
        if (inventory == null) {
            return new Object[]{null, "no item"};
        }

        final int selectedSlot = host.selectedSlot();
        if (selectedSlot < 0 || selectedSlot >= inventory.getContainerSize()) {
            return new Object[]{null, "no item"};
        }

        final ItemStack stack = inventory.getItem(selectedSlot);
        if (stack.isEmpty()) {
            return new Object[]{null, "no item"};
        }

        final int extractedExperience = extractedExperience(stack);
        if (extractedExperience <= 0) {
            return new Object[]{null, "could not extract experience from item"};
        }

        final ItemStack consumed = inventory.removeItem(selectedSlot, 1);
        if (consumed.isEmpty()) {
            return new Object[]{null, "could not consume item"};
        }

        addExperience(extractedExperience * CONSTANT_XP_GROWTH);
        return new Object[]{true};
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        experience = Math.max(0D, tag.getDouble(TAG_EXPERIENCE));
        updateXpInfo();
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        tag.putDouble(TAG_EXPERIENCE, experience);
    }

    public static double xpForLevel(final int level) {
        if (level <= 0) {
            return 0D;
        }
        return BASE_XP_TO_LEVEL + Math.pow(level * CONSTANT_XP_GROWTH, EXPONENTIAL_XP_GROWTH);
    }

    private void updateXpInfo() {
        level = calculateLevelFromExperience(experience);
        if (node() instanceof Connector connector) {
            connector.setLocalBufferSize(BUFFER_PER_LEVEL * level);
        }
    }

    private static int calculateLevelFromExperience(final double experience) {
        if (experience <= BASE_XP_TO_LEVEL) {
            return 0;
        }
        final double rawLevel = Math.pow(experience - BASE_XP_TO_LEVEL, 1D / EXPONENTIAL_XP_GROWTH) / CONSTANT_XP_GROWTH;
        return Math.max(0, Math.min((int) rawLevel, MAX_LEVEL));
    }

    private int extractedExperience(final ItemStack stack) {
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            final Level level = host.world();
            final RandomSource random = level == null ? RandomSource.create() : level.random;
            return 3 + random.nextInt(5) + random.nextInt(5);
        }

        int experience = 0;
        for (final var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            experience += entry.getKey().value().getMinCost(entry.getIntValue());
        }
        return experience;
    }

    private static double calculateExperienceLevel(final int level, final double experience) {
        if (level >= MAX_LEVEL) {
            return MAX_LEVEL;
        }
        final double xpNeeded = xpForLevel(level + 1) - xpForLevel(level);
        final double xpProgress = Math.max(0D, experience - xpForLevel(level));
        return level + xpProgress / xpNeeded;
    }
}
