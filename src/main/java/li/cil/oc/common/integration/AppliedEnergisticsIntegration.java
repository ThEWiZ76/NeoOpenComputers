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
            registry.add(new BlockInterfaceDriver());
            registry.add(new PartInterfaceDriver());
            registry.add(new ImportBusDriver());
            registry.add(new ExportBusDriver());
            registry.add((li.cil.oc.api.driver.EnvironmentProvider) stack -> {
                if (stack == null || stack.isEmpty()) return null;
                final var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (!id.getNamespace().equals("ae2")) return null;
                return switch (id.getPath()) {
                    case "controller" -> Environment.class;
                    case "interface", "cable_interface" -> InterfaceEnvironment.class;
                    case "import_bus" -> ImportEnvironment.class;
                    case "export_bus" -> ExportEnvironment.class;
                    default -> null;
                };
            });
            li.cil.oc.common.integration.ae2.CraftingStatusValue.register();
        }

        // Compound adapter persistence keys entries by driver type, so each device kind needs a distinct type.
        private static final class BlockInterfaceDriver extends ConfigDriver { BlockInterfaceDriver() { super("interface", false); } }
        private static final class PartInterfaceDriver extends ConfigDriver { PartInterfaceDriver() { super("cable_interface", true); } }
        private static final class ImportBusDriver extends ConfigDriver { ImportBusDriver() { super("import_bus", true); } }
        private static final class ExportBusDriver extends ConfigDriver { ExportBusDriver() { super("export_bus", true); } }

        private abstract static class ConfigDriver implements DriverBlock {
            private final String kind;
            private final boolean parts;
            ConfigDriver(final String kind, final boolean parts) {
                this.kind = kind; this.parts = parts;
            }
            @Override public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
                if (world == null || pos == null || !world.hasChunkAt(pos)) return false;
                final var tile = world.getBlockEntity(pos);
                if (!parts) return tile instanceof appeng.helpers.IConfigInvHost
                    && BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).equals(ResourceLocation.fromNamespaceAndPath("ae2", kind));
                if (!(tile instanceof appeng.api.parts.IPartHost host)) return false;
                for (final var face : Direction.values()) if (matches(host.getPart(face), kind)) return true;
                return false;
            }
            @Override public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                if (!worksWith(world, pos, side)) return null;
                final var tile = world.getBlockEntity(pos);
                return kind.equals("interface") || kind.equals("cable_interface") ? new InterfaceEnvironment(tile, side, parts)
                    : kind.equals("import_bus") ? new ImportEnvironment(tile) : new ExportEnvironment(tile);
            }
        }

        private static boolean matches(final appeng.api.parts.IPart part, final String kind) {
            return part != null && part instanceof appeng.helpers.IConfigInvHost
                && BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem()).equals(ResourceLocation.fromNamespaceAndPath("ae2", kind));
        }

        private static void checkTile(final BlockEntity tile) {
            final var world = tile.getLevel();
            if (world == null || tile.isRemoved() || !world.hasChunkAt(tile.getBlockPos()) || world.getBlockEntity(tile.getBlockPos()) != tile) {
                throw new IllegalStateException("no controller");
            }
        }

        private static Direction face(final Arguments args, final int index) {
            final int side = args.checkInteger(index);
            if (side < 0 || side > 5) throw new IllegalArgumentException("invalid side");
            return Direction.from3DDataValue(side);
        }

        private static appeng.helpers.externalstorage.GenericStackInv partConfig(final BlockEntity tile, final String kind, final Direction side) {
            checkTile(tile);
            final var part = ((appeng.api.parts.IPartHost) tile).getPart(side);
            return matches(part, kind) ? ((appeng.helpers.IConfigInvHost) part).getConfig() : null;
        }

        private static int configSlot(final appeng.helpers.externalstorage.GenericStackInv config, final int oneBased) {
            if (oneBased < 1 || oneBased > config.size()) throw new IllegalArgumentException("invalid slot");
            return oneBased - 1;
        }

        private static Object[] getConfig(final appeng.helpers.externalstorage.GenericStackInv config, final Arguments args, final int offset) {
            if (config == null) return new Object[]{null, "no matching part"};
            final int slot = configSlot(config, args.optInteger(offset, 1));
            final var key = config.getKey(slot);
            if (key == null) return new Object[]{null};
            final Object stack = key instanceof appeng.api.stacks.AEItemKey item ? item.toStack(1)
                : key instanceof appeng.api.stacks.AEFluidKey fluid ? fluid.toStack(1) : null;
            if (stack == null) return new Object[]{null};
            final var description = new LinkedHashMap<Object, Object>((Map<?, ?>) OpenComputersApi.convert(new Object[]{stack})[0]);
            description.put(key instanceof appeng.api.stacks.AEItemKey ? "size" : "amount", Math.max(1, config.getAmount(slot)));
            return new Object[]{description};
        }

        private static Object[] setConfig(final li.cil.oc.api.network.Node node, final appeng.helpers.externalstorage.GenericStackInv config,
                                          final Context context, final Arguments args, final int offset) {
            if (config == null) return new Object[]{null, "no matching part"};
            final int slot = configSlot(config, args.isString(offset) ? 1 : args.optInteger(offset, 1));
            appeng.api.stacks.GenericStack configured = null;
            if (args.count() > offset + 1) {
                final int addressIndex = args.isString(offset) ? offset : offset + 1;
                final String address = args.checkString(addressIndex);
                final var other = node.network() == null ? null : node.network().node(address);
                if (!(other instanceof Component component) || !other.canBeReachedFrom(node)) throw new IllegalArgumentException("no such component");
                if (!(component.host() instanceof Database database)) throw new IllegalArgumentException("not a database");
                final int entry = args.checkInteger(addressIndex + 1);
                if (entry < 1 || entry > database.size()) throw new IllegalArgumentException("invalid slot");
                final var stack = database.getStackInSlot(entry - 1).copy();
                final int size = args.optInteger(addressIndex + 2, 1);
                if (!stack.isEmpty() && size > 0) configured = new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(stack), Math.min(size, stack.getMaxStackSize()));
            }
            config.setStack(slot, configured);
            if (context != null) context.pause(0.5);
            return new Object[]{true};
        }

        public static final class InterfaceEnvironment extends Environment {
            private final boolean parts;
            private final Direction networkSide;
            private final appeng.api.parts.IPart networkPart;

            InterfaceEnvironment(final BlockEntity tile, final Direction adapterSide, final boolean parts) {
                super(tile, null, "me_interface", parts ? 0 : 5);
                this.parts = parts;
                Direction selected = adapterSide == null ? null : adapterSide.getOpposite();
                if (parts) {
                    final var host = (appeng.api.parts.IPartHost) tile;
                    if (selected == null || !matches(host.getPart(selected), "cable_interface")) {
                        selected = null;
                        for (final var candidate : Direction.values()) if (matches(host.getPart(candidate), "cable_interface")) { selected = candidate; break; }
                    }
                    networkPart = selected == null ? null : host.getPart(selected);
                } else networkPart = null;
                networkSide = selected;
            }

            @Override protected appeng.api.networking.IGridNode gridNode() {
                checkTile(tile);
                if (!parts) {
                    final var node = ((appeng.api.networking.security.IActionHost) tile).getActionableNode();
                    if (node == null) throw new IllegalStateException("no ae grid");
                    return node;
                }
                final var host = (appeng.api.parts.IPartHost) tile;
                if (networkSide == null || host.getPart(networkSide) != networkPart || !matches(networkPart, "cable_interface")
                    || networkPart.getGridNode() == null) throw new IllegalStateException("no ae grid");
                return networkPart.getGridNode();
            }

            @Override protected li.cil.oc.api.machine.Value craftable(final appeng.api.stacks.AEItemKey item, final long count) {
                return parts ? new li.cil.oc.common.integration.ae2.CraftableValue(tile, networkSide, item, count, "ae2:cable_interface")
                    : super.craftable(item, count);
            }

            @Callback(doc = "function([side:number,][slot:number]):table -- Get a block interface or side-specific panel configuration.")
            public Object[] getInterfaceConfiguration(final Context context, final Arguments args) {
                checkTile(tile);
                return getConfig(parts ? partConfig(tile, "cable_interface", face(args, 0)) : ((appeng.helpers.IConfigInvHost) tile).getConfig(), args, parts ? 1 : 0);
            }
            @Callback(doc = "function([side:number,][slot:number][, database:string, entry:number, size:number=1]):boolean -- Configure ghost items from a database description, or clear the slot.")
            public Object[] setInterfaceConfiguration(final Context context, final Arguments args) {
                checkTile(tile);
                return setConfig(node(), parts ? partConfig(tile, "cable_interface", face(args, 0)) : ((appeng.helpers.IConfigInvHost) tile).getConfig(), context, args, parts ? 1 : 0);
            }
        }

        public abstract static class BusEnvironment extends AbstractManagedEnvironment implements NamedBlock {
            protected final BlockEntity tile;
            private final String name;
            private final int priority;
            BusEnvironment(final BlockEntity tile, final String name, final int priority) {
                this.tile = tile; this.name = name; this.priority = priority;
                setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent(name, Visibility.Network).create());
            }
            @Override public String preferredName() { return name; }
            @Override public int priority() { return priority; }
        }

        public static final class ImportEnvironment extends BusEnvironment {
            ImportEnvironment(final BlockEntity tile) { super(tile, "me_importbus", 1); }
            @Callback(doc = "function(side:number[, slot:number]):table -- Get an import filter.")
            public Object[] getImportConfiguration(final Context context, final Arguments args) { return getConfig(partConfig(tile, "import_bus", face(args, 0)), args, 1); }
            @Callback(doc = "function(side:number[, slot:number][, database:string, entry:number, size:number=1]):boolean -- Configure or clear an import filter.")
            public Object[] setImportConfiguration(final Context context, final Arguments args) { return setConfig(node(), partConfig(tile, "import_bus", face(args, 0)), context, args, 1); }
        }

        public static final class ExportEnvironment extends BusEnvironment {
            ExportEnvironment(final BlockEntity tile) { super(tile, "me_exportbus", 2); }
            @Callback(doc = "function(side:number[, slot:number]):table -- Get an export filter.")
            public Object[] getExportConfiguration(final Context context, final Arguments args) { return getConfig(partConfig(tile, "export_bus", face(args, 0)), args, 1); }
            @Callback(doc = "function(side:number[, slot:number][, database:string, entry:number, size:number=1]):boolean -- Configure or clear an export filter.")
            public Object[] setExportConfiguration(final Context context, final Arguments args) { return setConfig(node(), partConfig(tile, "export_bus", face(args, 0)), context, args, 1); }

            @Callback(doc = "function(side:number[, slot:number]):number -- Export filtered ME items into an adjacent inventory, optionally into one slot.")
            public Object[] exportIntoSlot(final Context context, final Arguments args) {
                checkTile(tile);
                final var side = face(args, 0);
                final var part = ((appeng.api.parts.IPartHost) tile).getPart(side);
                if (!matches(part, "export_bus") || !(part instanceof appeng.parts.automation.ExportBusPart bus)) return new Object[]{null, "no export bus"};
                final var world = tile.getLevel();
                final var targetPos = tile.getBlockPos().relative(side);
                if (!world.hasChunkAt(targetPos)) return new Object[]{null, "no inventory"};
                final var target = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, targetPos, side.getOpposite());
                if (target == null) return new Object[]{null, "no inventory"};
                final int slot;
                if (args.optAny(1, null) != null) {
                    final int oneBased = args.checkInteger(1);
                    if (oneBased < 1 || oneBased > target.getSlots()) throw new IllegalArgumentException("invalid slot");
                    slot = oneBased - 1;
                } else slot = -1;
                final var node = bus.getGridNode();
                if (node == null || !node.isActive()) return new Object[]{null, "no items moved"};
                final var grid = node.getGrid();
                final var storage = grid.getStorageService().getInventory();
                final var source = appeng.api.networking.security.IActionSource.ofMachine(bus);
                final int budget = switch (bus.getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)) {
                    case 0 -> 1;
                    case 1 -> 8;
                    case 2 -> 32;
                    case 3 -> 64;
                    default -> 96;
                };
                final boolean fuzzy = bus.isUpgradedWith(appeng.core.definitions.AEItems.FUZZY_CARD);
                final var mode = bus.getConfigManager().getSetting(appeng.api.config.Settings.FUZZY_MODE);
                final var available = storage.getAvailableStacks();
                int moved = 0;
                for (int filter = 0; filter < bus.getConfig().size() && moved < budget; filter++) {
                    if (!(bus.getConfig().getKey(filter) instanceof appeng.api.stacks.AEItemKey key)) continue;
                    final var candidates = fuzzy ? available.findFuzzy(key, mode) : java.util.List.of(
                        new it.unimi.dsi.fastutil.objects.AbstractObject2LongMap.BasicEntry<appeng.api.stacks.AEKey>(key, available.get(key)));
                    for (final var candidate : candidates) {
                        if (moved >= budget) break;
                        if (!(candidate.getKey() instanceof appeng.api.stacks.AEItemKey item) || candidate.getLongValue() <= 0) continue;
                        moved += li.cil.oc.common.integration.ae2.ItemExport.transfer(grid.getEnergyService(), storage, item, source, target, slot,
                            (int) Math.min(budget - moved, candidate.getLongValue()),
                            stack -> net.minecraft.world.Containers.dropItemStack(world, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, stack));
                    }
                }
                if (moved == 0) return new Object[]{null, "no items moved"};
                if (context != null) context.pause(0.25);
                return new Object[]{moved};
            }
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

        public static class Environment extends AbstractManagedEnvironment implements NamedBlock {
            protected final BlockEntity tile;
            private final Direction side;
            private final String name;
            private final int priority;

            Environment(final BlockEntity tile, final Direction side) {
                this(tile, side, "me_controller", 5);
            }
            Environment(final BlockEntity tile, final Direction side, final String name, final int priority) {
                this.tile = tile;
                this.side = side;
                this.name = name; this.priority = priority;
                setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent(name, Visibility.Network).create());
            }

            @Override public String preferredName() { return name; }
            @Override public int priority() { return priority; }

            protected appeng.api.networking.IGridNode gridNode() {
                final var world = tile.getLevel();
                final var pos = tile.getBlockPos();
                checkTile(tile);
                final var host = world.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
                final var gridNode = host == null ? null : host.getGridNode(side);
                if (gridNode == null) throw new IllegalStateException("no ae grid");
                return gridNode;
            }

            private appeng.api.networking.IGrid grid() { return gridNode().getGrid(); }
            protected li.cil.oc.api.machine.Value craftable(final appeng.api.stacks.AEItemKey item, final long count) {
                return new li.cil.oc.common.integration.ae2.CraftableValue(tile, side, item, count);
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
                            result.add(craftable(item, outputCount));
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
