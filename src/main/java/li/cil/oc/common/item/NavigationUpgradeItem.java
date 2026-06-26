package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.NavigationUpgradeEnvironment;
import li.cil.oc.common.component.NavigationUpgradeEnvironment.NavigationMapData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class NavigationUpgradeItem extends Item implements HostAware {
    public NavigationUpgradeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        return new NavigationUpgradeEnvironment(host, mapData(stack, host == null ? null : host.world()));
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 1;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }

    public static NavigationMapData mapData(final ItemStack stack, final Level level) {
        return NavigationMapData.fromDataTag(ItemDriverData.dataTag(stack), level);
    }

    public static void storeMapData(final ItemStack stack, final NavigationMapData mapData) {
        if (stack == null || stack.isEmpty() || mapData == null) {
            return;
        }
        mapData.saveToDataTag(ItemDriverData.dataTag(stack));
    }

    public static void copyMapData(final ItemStack navigationUpgrade, final ItemStack mapStack, final Level level) {
        if (navigationUpgrade == null || navigationUpgrade.isEmpty() || mapStack == null || mapStack.isEmpty()) {
            return;
        }
        final MapId mapId = mapStack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            return;
        }
        final MapItemSavedData savedData = level == null ? null : net.minecraft.world.item.MapItem.getSavedData(mapId, level);
        final NavigationMapData mapData = savedData == null
            ? NavigationMapData.fromMapId(mapId.id(), null)
            : NavigationMapData.fromSavedData(savedData, mapId.id());
        storeMapData(navigationUpgrade, mapData);
    }
}
