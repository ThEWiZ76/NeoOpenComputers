package li.cil.oc.common;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.driver.CompoundBlockDriver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DriverRegistry implements DriverAPI {
    private final List<DriverBlock> blockDrivers = new ArrayList<>();
    private final List<DriverItem> itemDrivers = new ArrayList<>();
    private final List<Converter> converters = new ArrayList<>();
    private final List<EnvironmentProvider> environmentProviders = new ArrayList<>();
    private final List<InventoryProvider> inventoryProviders = new ArrayList<>();
    private final List<HostBlacklistEntry> hostBlacklist = new ArrayList<>();
    private boolean locked;

    @Override
    public void add(final DriverBlock driver) {
        ensureUnlocked("drivers");
        if (!blockDrivers.contains(driver)) {
            blockDrivers.add(driver);
        }
    }

    @Override
    public void add(final DriverItem driver) {
        ensureUnlocked("drivers");
        if (!itemDrivers.contains(driver)) {
            itemDrivers.add(driver);
        }
    }

    @Override
    public void add(final Converter converter) {
        ensureUnlocked("converters");
        if (!converters.contains(converter)) {
            converters.add(converter);
        }
    }

    @Override
    public void add(final EnvironmentProvider provider) {
        ensureUnlocked("environment providers");
        if (!environmentProviders.contains(provider)) {
            environmentProviders.add(provider);
        }
    }

    @Override
    public void add(final InventoryProvider provider) {
        ensureUnlocked("inventory providers");
        if (!inventoryProviders.contains(provider)) {
            inventoryProviders.add(provider);
        }
    }

    @Override
    public DriverBlock driverFor(final Level world, final BlockPos pos, final Direction side) {
        final List<DriverBlock> matches = new ArrayList<>();
        for (final DriverBlock driver : blockDrivers) {
            if (driver.worksWith(world, pos, side)) {
                matches.add(driver);
            }
        }
        return switch (matches.size()) {
            case 0 -> null;
            case 1 -> matches.getFirst();
            default -> new CompoundBlockDriver(matches);
        };
    }

    @Override
    public DriverItem driverFor(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        if (isHostBlacklisted(stack, host)) {
            return null;
        }
        final List<DriverItem> hostAwareMatches = new ArrayList<>();
        for (final DriverItem driver : itemDrivers) {
            if (driver instanceof HostAware) {
                if (driver.worksWith(stack)) {
                    hostAwareMatches.add(driver);
                }
            }
        }
        if (!hostAwareMatches.isEmpty()) {
            for (final DriverItem driver : hostAwareMatches) {
                if (((HostAware) driver).worksWith(stack, host)) {
                    return driver;
                }
            }
            return null;
        }
        return driverFor(stack);
    }

    @Override
    public DriverItem driverFor(final ItemStack stack) {
        for (final DriverItem driver : itemDrivers) {
            if (driver.worksWith(stack)) {
                return driver;
            }
        }
        return null;
    }

    @Override
    @Deprecated
    public Class<?> environmentFor(final ItemStack stack) {
        for (final EnvironmentProvider provider : environmentProviders) {
            final Class<?> environment = provider.getEnvironment(stack);
            if (environment != null) {
                return environment;
            }
        }
        return null;
    }

    @Override
    public Set<Class<?>> environmentsFor(final ItemStack stack) {
        final Set<Class<?>> environments = new LinkedHashSet<>();
        for (final EnvironmentProvider provider : environmentProviders) {
            final Class<?> environment = provider.getEnvironment(stack);
            if (environment != null) {
                environments.add(environment);
            }
        }
        return environments;
    }

    @Override
    public IItemHandler itemHandlerFor(final ItemStack stack, final Player player) {
        for (final InventoryProvider provider : inventoryProviders) {
            if (provider.worksWith(stack, player)) {
                final Container inventory = provider.getInventory(stack, player);
                return inventory == null ? null : new InvWrapper(inventory);
            }
        }
        return null;
    }

    @Override
    public Collection<DriverItem> itemDrivers() {
        return List.copyOf(itemDrivers);
    }

    public void blacklistHost(final ItemStack stack, final Class<?> host) {
        if (stack == null || stack.isEmpty() || host == null) {
            return;
        }
        for (HostBlacklistEntry entry : hostBlacklist) {
            if (ItemStack.isSameItem(entry.stack, stack)) {
                entry.hosts.add(host);
                return;
            }
        }
        ItemStack key = stack.copy();
        key.setCount(1);
        final Set<Class<?>> hosts = new LinkedHashSet<>();
        hosts.add(host);
        hostBlacklist.add(new HostBlacklistEntry(key, hosts));
    }

    int converterCount() {
        return converters.size();
    }

    Object[] convert(final Object[] values) {
        if (values == null) {
            return null;
        }
        final Object[] converted = new Object[values.length];
        for (int index = 0; index < values.length; index++) {
            converted[index] = convertRecursively(values[index], new IdentityHashMap<>());
        }
        return converted;
    }

    void lockRegistrations() {
        locked = true;
    }

    private void ensureUnlocked(final String type) {
        if (locked) {
            throw new IllegalStateException("Please register all " + type + " in the init phase.");
        }
    }

    private boolean isHostBlacklisted(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        if (stack == null || stack.isEmpty() || host == null) {
            return false;
        }
        for (HostBlacklistEntry entry : hostBlacklist) {
            if (ItemStack.isSameItem(entry.stack, stack)) {
                for (Class<?> blacklistedHost : entry.hosts) {
                    if (blacklistedHost.isAssignableFrom(host)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private record HostBlacklistEntry(ItemStack stack, Set<Class<?>> hosts) {
    }

    private Object convertRecursively(final Object value, final IdentityHashMap<Object, Object> memo) {
        if (memo.containsKey(value)) {
            return memo.get(value);
        }
        return switch (value) {
            case null -> null;
            case Boolean ignored -> value;
            case Byte ignored -> value;
            case Character ignored -> value;
            case Short ignored -> value;
            case Integer ignored -> value;
            case Long ignored -> value;
            case Float ignored -> value;
            case Double ignored -> value;
            case String ignored -> value;
            case byte[] ignored -> value;
            case boolean[] ignored -> value;
            case char[] ignored -> value;
            case short[] ignored -> value;
            case int[] ignored -> value;
            case long[] ignored -> value;
            case float[] ignored -> value;
            case double[] ignored -> value;
            case Boolean[] ignored -> value;
            case Byte[] ignored -> value;
            case Character[] ignored -> value;
            case Short[] ignored -> value;
            case Integer[] ignored -> value;
            case Long[] ignored -> value;
            case Float[] ignored -> value;
            case Double[] ignored -> value;
            case String[] ignored -> value;
            case Value ignored -> value;
            case Number number -> number.doubleValue();
            case Map<?, ?> map -> convertMap(value, map, memo);
            case Iterable<?> iterable -> convertIterable(value, iterable, memo);
            default -> convertUnknown(value, memo);
        };
    }

    private Object convertUnknown(final Object value, final IdentityHashMap<Object, Object> memo) {
        if (value.getClass().isArray()) {
            return convertArray(value, memo);
        }
        final Map<Object, Object> converted = new LinkedHashMap<>();
        memo.put(value, converted);
        for (Converter converter : converters) {
            try {
                converter.convert(value, converted);
            } catch (Throwable t) {
                NeoOpenComputers.LOGGER.warn("Type converter threw an exception.", t);
            }
        }
        if (converted.isEmpty()) {
            final String fallback = value.toString();
            memo.put(value, fallback);
            return fallback;
        }
        final Map<Object, Object> raw = new LinkedHashMap<>(converted);
        converted.clear();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            final Object key = convertRecursively(entry.getKey(), memo);
            if (key != null) {
                converted.put(key, convertRecursively(entry.getValue(), memo));
            }
        }
        if (converted.size() == 1 && converted.containsKey("oc:flatten")) {
            final Object flattened = converted.get("oc:flatten");
            memo.put(value, flattened);
            return flattened;
        }
        return converted;
    }

    private Object[] convertArray(final Object array, final IdentityHashMap<Object, Object> memo) {
        final int length = Array.getLength(array);
        final Object[] converted = new Object[length];
        memo.put(array, converted);
        for (int index = 0; index < length; index++) {
            converted[index] = convertRecursively(Array.get(array, index), memo);
        }
        return converted;
    }

    private Object[] convertIterable(final Object iterableObject, final Iterable<?> iterable, final IdentityHashMap<Object, Object> memo) {
        final List<Object> converted = new ArrayList<>();
        memo.put(iterableObject, converted);
        for (Object entry : iterable) {
            converted.add(convertRecursively(entry, memo));
        }
        final Object[] array = converted.toArray();
        memo.put(iterableObject, array);
        return array;
    }

    private Map<Object, Object> convertMap(final Object mapObject, final Map<?, ?> map, final IdentityHashMap<Object, Object> memo) {
        final Map<Object, Object> converted = new LinkedHashMap<>();
        memo.put(mapObject, converted);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            final Object key = convertRecursively(entry.getKey(), memo);
            if (key != null) {
                converted.put(key, convertRecursively(entry.getValue(), memo));
            }
        }
        return converted;
    }
}
