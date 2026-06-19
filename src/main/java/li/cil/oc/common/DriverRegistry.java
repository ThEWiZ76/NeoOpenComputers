package li.cil.oc.common;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DriverRegistry implements DriverAPI {
    private final List<DriverBlock> blockDrivers = new ArrayList<>();
    private final List<DriverItem> itemDrivers = new ArrayList<>();
    private final List<Converter> converters = new ArrayList<>();
    private final List<EnvironmentProvider> environmentProviders = new ArrayList<>();
    private final List<InventoryProvider> inventoryProviders = new ArrayList<>();

    @Override
    public void add(final DriverBlock driver) {
        blockDrivers.add(driver);
    }

    @Override
    public void add(final DriverItem driver) {
        itemDrivers.add(driver);
    }

    @Override
    public void add(final Converter converter) {
        converters.add(converter);
    }

    @Override
    public void add(final EnvironmentProvider provider) {
        environmentProviders.add(provider);
    }

    @Override
    public void add(final InventoryProvider provider) {
        inventoryProviders.add(provider);
    }

    @Override
    public DriverBlock driverFor(final Level world, final BlockPos pos, final Direction side) {
        for (final DriverBlock driver : blockDrivers) {
            if (driver.worksWith(world, pos, side)) {
                return driver;
            }
        }
        return null;
    }

    @Override
    public DriverItem driverFor(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        for (final DriverItem driver : itemDrivers) {
            if (driver instanceof final HostAware hostAware) {
                if (hostAware.worksWith(stack, host)) {
                    return driver;
                }
            } else if (driver.worksWith(stack)) {
                return driver;
            }
        }
        return null;
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

    int converterCount() {
        return converters.size();
    }
}
