package li.cil.oc.common.integration;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Foreign API linkage stays behind the optional installed-mod guard. */
public final class AppliedEnergisticsIntegration {
    private AppliedEnergisticsIntegration() { }

    public static void register(final DriverRegistry registry) {
        if (ModList.get() != null && ModList.get().isLoaded("ae2")) Loaded.register(registry);
    }

    private static final class Loaded {
        private static final ResourceLocation CONTROLLER = ResourceLocation.fromNamespaceAndPath("ae2", "controller");

        static void register(final DriverRegistry registry) {
            registry.add(new ControllerDriver());
            li.cil.oc.common.integration.ae2.CraftingStatusValue.register();
        }

        private static final class ControllerDriver implements DriverBlock {
            @Override
            public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
                return world != null && pos != null && world.hasChunkAt(pos)
                    && BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).equals(CONTROLLER)
                    && world.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null) != null;
            }

            @Override
            public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                return worksWith(world, pos, side) ? new Environment(world.getBlockEntity(pos), side) : null;
            }
        }

        public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
            private final BlockEntity tile;
            private final Direction side;

            Environment(final BlockEntity tile, final Direction side) {
                this.tile = tile;
                this.side = side;
                setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent(preferredName(), Visibility.Network).create());
            }

            @Override public String preferredName() { return "me_controller"; }
            @Override public int priority() { return 5; }

            private appeng.api.networking.IGrid grid() {
                final var world = tile.getLevel();
                final var pos = tile.getBlockPos();
                if (world == null || tile.isRemoved() || !world.hasChunkAt(pos) || world.getBlockEntity(pos) != tile) {
                    throw new IllegalStateException("no controller");
                }
                final var host = world.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                final var gridNode = host == null ? null : host.getGridNode(side);
                if (gridNode == null) throw new IllegalStateException("no ae grid");
                return gridNode.getGrid();
            }

            @Callback(doc = "function():table -- Get the available crafting CPUs.")
            public Object[] getCpus(final Context context, final Arguments args) {
                final var result = new ArrayList<Map<String, Object>>();
                for (final var cpu : grid().getCraftingService().getCpus()) {
                    result.add(Map.of("name", cpu.getName() == null ? "" : cpu.getName().getString(), "storage", cpu.getAvailableStorage(),
                        "coprocessors", cpu.getCoProcessors(), "busy", cpu.isBusy()));
                }
                return new Object[]{result.toArray()};
            }

            private Map<appeng.api.stacks.AEItemKey, Long> items(final appeng.api.networking.IGrid grid) {
                final var result = new LinkedHashMap<appeng.api.stacks.AEItemKey, Long>();
                for (final var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
                    if (entry.getKey() instanceof appeng.api.stacks.AEItemKey item && entry.getLongValue() > 0) result.put(item, entry.getLongValue());
                }
                for (final var key : grid.getCraftingService().getCraftables(appeng.api.storage.AEKeyFilter.none())) {
                    if (key instanceof appeng.api.stacks.AEItemKey item) result.putIfAbsent(item, 0L);
                }
                return result;
            }

            private Map<Object, Object> describe(final appeng.api.stacks.AEItemKey item, final long amount, final appeng.api.networking.IGrid grid) {
                final var result = description(item.toStack(1));
                result.put("size", amount);
                result.put("isCraftable", grid.getCraftingService().isCraftable(item));
                return result;
            }

            private Map<Object, Object> description(final Object value) {
                return new LinkedHashMap<>((Map<?, ?>) OpenComputersApi.convert(new Object[]{value})[0]);
            }

            @Callback(doc = "function([filter:table]):table -- Get stored and craftable item descriptions.")
            public Object[] getItemsInNetwork(final Context context, final Arguments args) {
                final var grid = grid();
                final var filter = args.optTable(0, Map.of());
                final var result = new ArrayList<Map<Object, Object>>();
                items(grid).forEach((key, amount) -> {
                    final var item = describe(key, amount, grid);
                    if (DescriptionFilter.matches(item, filter)) result.add(item);
                });
                return new Object[]{result.toArray()};
            }

            @Callback(doc = "function([filter:table]):table -- Get known item recipes as craftable userdata.")
            public Object[] getCraftables(final Context context, final Arguments args) {
                final var grid = grid();
                final var filter = args.optTable(0, Map.of());
                final var result = new ArrayList<li.cil.oc.api.machine.Value>();
                for (final var key : grid.getCraftingService().getCraftables(appeng.api.storage.AEKeyFilter.none())) {
                    if (key instanceof appeng.api.stacks.AEItemKey item) {
                        final long outputCount = recipeOutputCount(grid, item);
                        if (DescriptionFilter.matches(describe(item, outputCount, grid), filter)) {
                            result.add(new li.cil.oc.common.integration.ae2.CraftableValue(tile, side, item, outputCount));
                        }
                    }
                }
                return new Object[]{result.toArray()};
            }

            private long recipeOutputCount(final appeng.api.networking.IGrid grid, final appeng.api.stacks.AEItemKey item) {
                return grid.getCraftingService().getCraftingFor(item).stream()
                    .flatMap(pattern -> pattern.getOutputs().stream()).filter(output -> output.what().equals(item))
                    .mapToLong(appeng.api.stacks.GenericStack::amount).findFirst().orElse(0);
            }

            @Callback(doc = "function():table -- Get fluid descriptions; amounts are millibuckets.")
            public Object[] getFluidsInNetwork(final Context context, final Arguments args) {
                final var result = new ArrayList<Map<Object, Object>>();
                for (final var entry : grid().getStorageService().getInventory().getAvailableStacks()) {
                    if (entry.getKey() instanceof appeng.api.stacks.AEFluidKey fluid && entry.getLongValue() > 0) {
                        final var description = description(fluid.toStack(1));
                        description.put("amount", entry.getLongValue());
                        result.add(description);
                    }
                }
                return new Object[]{result.toArray()};
            }

            @Callback(doc = "function([filter:table, dbAddress:string, startSlot:number, count:number]):boolean -- Copy matching item descriptions into empty database slots.")
            public Object[] store(final Context context, final Arguments args) {
                final var grid = grid();
                final var filter = args.optTable(0, Map.of());
                final var database = database(args.optString(1, null));
                final int start = args.optInteger(2, 1) - 1;
                if (start < 0 || start >= database.size()) throw new IllegalArgumentException("invalid slot");
                int remaining = Math.max(0, args.optInteger(3, Integer.MAX_VALUE));
                int slot = start;
                for (final var entry : items(grid).entrySet()) {
                    if (remaining == 0) break;
                    if (!DescriptionFilter.matches(describe(entry.getKey(), entry.getValue(), grid), filter)) continue;
                    while (slot < database.size() && !database.getStackInSlot(slot).isEmpty()) slot++;
                    if (slot == database.size()) break;
                    final long quantity = entry.getValue() == 0 ? recipeOutputCount(grid, entry.getKey()) : entry.getValue();
                    database.setStackInSlot(slot++, entry.getKey().toStack((int) Math.min(Integer.MAX_VALUE, quantity)));
                    remaining--;
                }
                return new Object[]{true};
            }

            private Database database(final String address) {
                if (address != null) {
                    final var other = node().network() == null ? null : node().network().node(address);
                    if (!(other instanceof Component component) || !other.canBeReachedFrom(node())) throw new IllegalArgumentException("no such component");
                    if (!(component.host() instanceof Database database)) throw new IllegalArgumentException("not a database");
                    return database;
                }
                for (final var other : node().reachableNodes()) {
                    if (other.host() instanceof Database database) return database;
                }
                throw new IllegalArgumentException("no database upgrade found");
            }

            @Callback(doc = "function():number -- Get average power injection.")
            public Object[] getAvgPowerInjection(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().getAvgPowerInjection()}; }
            @Callback(doc = "function():number -- Get average power usage.")
            public Object[] getAvgPowerUsage(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().getAvgPowerUsage()}; }
            @Callback(doc = "function():number -- Get idle power usage.")
            public Object[] getIdlePowerUsage(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().getIdlePowerUsage()}; }
            @Callback(doc = "function():number -- Get maximum stored power.")
            public Object[] getMaxStoredPower(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().getMaxStoredPower()}; }
            @Callback(doc = "function():number -- Get stored power.")
            public Object[] getStoredPower(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().getStoredPower()}; }
            @Callback(doc = "function():boolean -- Get network powered state.")
            public Object[] isNetworkPowered(final Context context, final Arguments args) { return new Object[]{grid().getEnergyService().isNetworkPowered()}; }
            @Callback(doc = "function():number -- Get network energy demand.")
            public Object[] getEnergyDemand(final Context context, final Arguments args) {
                if (context != null) context.consumeCallBudget(1.5);
                return new Object[]{grid().getEnergyService().getEnergyDemand(Double.MAX_VALUE)};
            }
        }
    }
}
