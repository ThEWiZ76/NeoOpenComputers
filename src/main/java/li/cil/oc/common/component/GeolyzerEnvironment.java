package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

public final class GeolyzerEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final int RANGE = 32;
    private static final int MAX_VOLUME = 64;
    private static final double SCAN_COST = 10D;
    private static final String COMPONENT_NAME = "geolyzer";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Geolyzer",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Terrain Analyzer MkII",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(RANGE)
    );

    private final EnvironmentHost host;

    public GeolyzerEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void onMessage(final Message message) {
        if (message == null || !"tablet.use".equals(message.name())) {
            return;
        }
        final Object[] data = message.data();
        if (data.length < 4 || !(data[0] instanceof CompoundTag nbt) || !(data[3] instanceof BlockPos blockPos)) {
            return;
        }
        if (!consumeEnergy()) {
            return;
        }
        final GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(host, Map.of(), blockPos);
        fillAnalyze(event);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        for (final Map.Entry<String, Object> entry : event.data.entrySet()) {
            switch (entry.getValue()) {
                case Number number -> nbt.putDouble(entry.getKey(), number.doubleValue());
                case String string when !string.isEmpty() -> nbt.putString(entry.getKey(), string);
                default -> {
                }
            }
        }
    }

    @Callback(doc = "function():boolean -- Returns whether there is a clear line of sight to the sky directly above.")
    public Object[] canSeeSky(final Context context, final Arguments args) {
        final Level level = level();
        return new Object[]{level != null && level.canSeeSky(hostPos().above())};
    }

    @Callback(doc = "function():boolean -- Return whether the sun is currently visible directly above.")
    public Object[] isSunVisible(final Context context, final Arguments args) {
        final Level level = level();
        return new Object[]{level != null && level.isDay() && level.canSeeSky(hostPos().above()) && !level.isRaining() && !level.isThundering()};
    }

    @Callback(doc = "function(x:number, z:number[, y:number, w:number, d:number, h:number][, ignoreReplaceable:boolean|options:table]):table -- Scans block hardness in the specified relative bounds.")
    public Object[] scan(final Context context, final Arguments args) {
        final ScanBounds bounds = scanBounds(args);
        if (bounds.volume() > MAX_VOLUME) {
            throw new IllegalArgumentException("volume too large (maximum is 64)");
        }
        if (bounds.outOfRange()) {
            throw new IllegalArgumentException("location out of bounds");
        }
        if (!consumeEnergy()) {
            return noEnergy();
        }
        final Map<?, ?> options = scanOptions(args, bounds.optionsIndex());
        final GeolyzerEvent.Scan event = new GeolyzerEvent.Scan(host, options, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        fillScan(event, includeReplaceable(options));
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return new Object[]{null, "scan was canceled"};
        }
        return new Object[]{event.data};
    }

    @Callback(doc = "function(side:number[,options:table]):table -- Get some information on a directly adjacent block.")
    public Object[] analyze(final Context context, final Arguments args) {
        final BlockPos target = relativeBlock(args.checkInteger(0));
        final Map<?, ?> options = args.optTable(1, Map.of());
        if (!consumeEnergy()) {
            return noEnergy();
        }
        final GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(host, options, target);
        fillAnalyze(event);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return new Object[]{null, "scan was canceled"};
        }
        return new Object[]{event.data};
    }

    @Callback(doc = "function(side:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack representation of the block on the specified side in a database component.")
    public Object[] store(final Context context, final Arguments args) {
        final BlockPos target = relativeBlock(args.checkInteger(0));
        if (!consumeEnergy()) {
            return noEnergy();
        }
        final Level level = level();
        if (level == null) {
            throw new IllegalStateException("no world");
        }
        final ItemStack stack = new ItemStack(level.getBlockState(target).getBlock().asItem());
        if (stack.isEmpty() || stack.is(Items.AIR)) {
            return new Object[]{null, "block has no registered item representation"};
        }
        final Database database = database(args.checkString(1));
        final int slot = args.checkInteger(2) - 1;
        if (slot < 0 || slot >= database.size()) {
            throw new IllegalArgumentException("slot index out of bounds");
        }
        final boolean overwritten = !database.getStackInSlot(slot).isEmpty();
        database.setStackInSlot(slot, stack);
        return new Object[]{overwritten};
    }

    private boolean consumeEnergy() {
        return node() instanceof ComponentConnector connector && connector.tryChangeBuffer(-SCAN_COST);
    }

    private void fillScan(final GeolyzerEvent.Scan event, final boolean includeReplaceable) {
        final Level level = level();
        if (level == null) {
            return;
        }
        int index = 0;
        final BlockPos origin = hostPos();
        for (int y = event.minY; y <= event.maxY; y++) {
            for (int z = event.minZ; z <= event.maxZ; z++) {
                for (int x = event.minX; x <= event.maxX; x++) {
                    event.data[index++] = hardness(level, origin.offset(x, y, z), includeReplaceable);
                }
            }
        }
    }

    private float hardness(final Level level, final BlockPos target, final boolean includeReplaceable) {
        if (!level.isLoaded(target)) {
            return 0.0F;
        }
        final BlockState state = level.getBlockState(target);
        if (state.isAir()) {
            return 0.0F;
        }
        if (!includeReplaceable && state.canBeReplaced()) {
            return 0.0F;
        }
        return state.getDestroySpeed(level, target);
    }

    private void fillAnalyze(final GeolyzerEvent.Analyze event) {
        final Level level = level();
        if (level == null || !level.isLoaded(event.pos)) {
            return;
        }
        final BlockState state = level.getBlockState(event.pos);
        final var blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        event.data.put("name", blockKey == null ? "minecraft:air" : blockKey.toString());
        event.data.put("hardness", state.getDestroySpeed(level, event.pos));
        event.data.put("color", state.getMapColor(level, event.pos).col);
        final Map<String, String> properties = new HashMap<>();
        state.getValues().forEach((property, value) -> properties.put(property.getName(), value.toString()));
        event.data.put("properties", properties);
        event.data.put("metadata", 0);
    }

    private BlockPos relativeBlock(final int side) {
        return hostPos().relative(globalSide(side));
    }

    private Direction globalSide(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        final Direction localSide = Direction.from3DDataValue(side);
        if (host instanceof Rotatable rotatable) {
            return rotatable.toGlobal(localSide);
        }
        return localSide;
    }

    private Database database(final String address) {
        if (node() == null || node().network() == null) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(node().network().node(address) instanceof Component component)) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(component.host() instanceof Database database)) {
            throw new IllegalArgumentException("not a database");
        }
        return database;
    }

    private Level level() {
        return host == null ? null : host.world();
    }

    private BlockPos hostPos() {
        if (host == null) {
            return BlockPos.ZERO;
        }
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }

    private static Object[] noEnergy() {
        return new Object[]{null, "not enough energy"};
    }

    private static Map<?, ?> scanOptions(final Arguments args, final int index) {
        if (args.isBoolean(index)) {
            return Map.of("includeReplaceable", !args.checkBoolean(index));
        }
        return args.optTable(index, Map.of());
    }

    private static boolean includeReplaceable(final Map<?, ?> options) {
        final Object value = options.get("includeReplaceable");
        return !(value instanceof Boolean include) || include;
    }

    private static ScanBounds scanBounds(final Arguments args) {
        final int minX = args.checkInteger(0);
        final int minZ = args.checkInteger(1);
        if (args.isInteger(2) && args.isInteger(3) && args.isInteger(4) && args.isInteger(5)) {
            final int minY = args.checkInteger(2);
            final int width = args.checkInteger(3);
            final int depth = args.checkInteger(4);
            final int height = args.checkInteger(5);
            final int maxX = minX + width - 1;
            final int maxY = minY + height - 1;
            final int maxZ = minZ + depth - 1;
            return new ScanBounds(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ), Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ), 6);
        }
        return new ScanBounds(minX, -32, minZ, minX, 31, minZ, 2);
    }

    private record ScanBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int optionsIndex) {
        int volume() {
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        boolean outOfRange() {
            return Math.abs(minX) > RANGE || Math.abs(maxX) > RANGE || Math.abs(minY) > RANGE || Math.abs(maxY) > RANGE || Math.abs(minZ) > RANGE || Math.abs(maxZ) > RANGE;
        }
    }
}
