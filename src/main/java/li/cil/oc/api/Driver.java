package li.cil.oc.api;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collection;
import java.util.Set;

public final class Driver {
    public static void add(final DriverBlock driver) {
        if (API.driver != null) {
            API.driver.add(driver);
        }
    }

    public static void add(final DriverItem driver) {
        if (API.driver != null) {
            API.driver.add(driver);
        }
    }

    public static void add(final Converter converter) {
        if (API.driver != null) {
            API.driver.add(converter);
        }
    }

    public static void add(final EnvironmentProvider provider) {
        if (API.driver != null) {
            API.driver.add(provider);
        }
    }

    public static void add(final InventoryProvider provider) {
        if (API.driver != null) {
            API.driver.add(provider);
        }
    }

    public static DriverBlock driverFor(final Level world, final BlockPos pos, final Direction side) {
        if (API.driver != null) {
            return API.driver.driverFor(world, pos, side);
        }
        return null;
    }

    public static DriverItem driverFor(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        if (API.driver != null) {
            return API.driver.driverFor(stack, host);
        }
        return null;
    }

    public static DriverItem driverFor(final ItemStack stack) {
        if (API.driver != null) {
            return API.driver.driverFor(stack);
        }
        return null;
    }

    @Deprecated
    public static Class<?> environmentFor(final ItemStack stack) {
        if (API.driver != null) {
            return API.driver.environmentFor(stack);
        }
        return null;
    }

    public static Set<Class<?>> environmentsFor(final ItemStack stack) {
        if (API.driver != null) {
            return API.driver.environmentsFor(stack);
        }
        return null;
    }

    public static IItemHandler itemHandlerFor(final ItemStack stack, final Player player) {
        if (API.driver != null) {
            return API.driver.itemHandlerFor(stack, player);
        }
        return null;
    }

    public static Collection<DriverItem> itemDrivers() {
        if (API.driver != null) {
            return API.driver.itemDrivers();
        }
        return null;
    }

    private Driver() {
    }
}
