package li.cil.oc.api.detail;

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

public interface DriverAPI {
    void add(DriverBlock driver);

    void add(DriverItem driver);

    void add(Converter converter);

    void add(EnvironmentProvider provider);

    void add(InventoryProvider provider);

    DriverBlock driverFor(Level world, BlockPos pos, Direction side);

    DriverItem driverFor(ItemStack stack, Class<? extends EnvironmentHost> host);

    DriverItem driverFor(ItemStack stack);

    @Deprecated
    Class<?> environmentFor(ItemStack stack);

    Set<Class<?>> environmentsFor(ItemStack stack);

    IItemHandler itemHandlerFor(ItemStack stack, Player player);

    Collection<DriverItem> itemDrivers();
}
