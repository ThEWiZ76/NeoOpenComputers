package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.NavigationUpgradeEnvironment;
import li.cil.oc.common.component.NavigationUpgradeEnvironment.NavigationMapData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class NavigationUpgradeItem extends Item implements HostAware {
    private static final String ORIGINAL_MAP_TAG = "oc:navigationMap";
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
        final var environment = new NavigationUpgradeEnvironment(host, mapData(stack, host == null ? null : host.world())) {
            @Override
            public void save(final CompoundTag data) {
                super.save(data);
                ItemDriverData.writeDataTag(stack, data);
            }
        };
        environment.load(dataTag(stack));
        return environment;
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
        copyMapData(navigationUpgrade, mapStack, level, level == null ? null : level.registryAccess());
    }

    public static void copyMapData(final ItemStack navigationUpgrade, final ItemStack mapStack, final Level level, final HolderLookup.Provider registries) {
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
        final var savedMap = ItemStack.OPTIONAL_CODEC.encodeStart(
            registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE), mapStack.copyWithCount(1)).getOrThrow();
        storeMapData(navigationUpgrade, mapData);
        // Keep the item snapshot outside driver data, which environments replace when saving their node/map state.
        final var root = navigationUpgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ORIGINAL_MAP_TAG, savedMap);
        navigationUpgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static ItemStack originalMap(final ItemStack navigationUpgrade, final HolderLookup.Provider registries) {
        final var root = navigationUpgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(ORIGINAL_MAP_TAG)) {
            final var map = ItemStack.OPTIONAL_CODEC.parse(
                registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE), root.getCompound(ORIGINAL_MAP_TAG))
                .result().orElse(ItemStack.EMPTY);
            return map.is(Items.FILLED_MAP) ? map.copyWithCount(1) : ItemStack.EMPTY;
        }
        // Earlier port builds retained only map coordinates/ID, so recover the known ID when no full snapshot exists.
        final var data = NavigationMapData.load(root.getCompound("oc:data"), NavigationMapData.DEFAULT);
        final var map = new ItemStack(Items.FILLED_MAP);
        if (data.mapId() != NavigationMapData.NO_MAP_ID) map.set(DataComponents.MAP_ID, new MapId(data.mapId()));
        return map;
    }
}
