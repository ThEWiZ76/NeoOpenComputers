package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CompoundBlockDriver implements DriverBlock {
    private final List<DriverBlock> drivers;

    public CompoundBlockDriver(final List<DriverBlock> drivers) {
        this.drivers = List.copyOf(drivers);
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        for (DriverBlock driver : drivers) {
            if (!driver.worksWith(world, pos, side)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        final List<CompoundBlockEnvironment.Entry> environments = new ArrayList<>();
        for (DriverBlock driver : drivers) {
            final ManagedEnvironment environment = driver.createEnvironment(world, pos, side);
            if (environment != null) {
                environments.add(new CompoundBlockEnvironment.Entry(driver.getClass().getName(), environment));
            }
        }
        if (environments.isEmpty()) {
            return null;
        }
        return new CompoundBlockEnvironment(cleanName(preferredName(environments)), environments);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof CompoundBlockDriver other
            && drivers.size() == other.drivers.size()
            && drivers.containsAll(other.drivers);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(drivers.stream().filter(Objects::nonNull).mapToInt(Object::hashCode).sorted().toArray());
    }

    private static String preferredName(final List<CompoundBlockEnvironment.Entry> environments) {
        NamedBlock best = null;
        for (CompoundBlockEnvironment.Entry entry : environments) {
            if (entry.environment() instanceof NamedBlock named && (best == null || named.priority() > best.priority())) {
                best = named;
            }
        }
        return best == null ? "component" : best.preferredName();
    }

    private static String cleanName(final String name) {
        final String nonEmptyName = name == null || name.isBlank() ? "component" : name;
        final String safeStart = nonEmptyName.matches("^[^a-zA-Z_].*") ? "_" + nonEmptyName : nonEmptyName;
        final String identifier = safeStart.replaceAll("[^\\w_]", "_").trim();
        return identifier.isEmpty() ? "component" : identifier.toLowerCase(Locale.ROOT);
    }
}
