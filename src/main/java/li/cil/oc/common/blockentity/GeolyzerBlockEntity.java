package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

public class GeolyzerBlockEntity extends BlockEntity implements Environment, EnvironmentHost, DeviceInfo {
    private static final String TAG_NODE = "node";
    private static final int MAX_VOLUME = 64;

    private Node node;

    public GeolyzerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.GEOLYZER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = createNode(this);
        }
        return node;
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
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
        final GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(this, Map.of(), blockPos);
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

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Geolyzer",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Terrain Analyzer MkII",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(ModSettings.geolyzerRange())
        );
    }

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return getBlockPos().getX() + 0.5D;
    }

    @Override
    public double yPosition() {
        return getBlockPos().getY() + 0.5D;
    }

    @Override
    public double zPosition() {
        return getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Callback(doc = "function():boolean -- Returns whether there is a clear line of sight to the sky directly above.")
    public Object[] canSeeSky(final Context context, final Arguments args) {
        return new Object[]{level != null && level.canSeeSky(getBlockPos().above())};
    }

    @Callback(doc = "function():boolean -- Return whether the sun is currently visible directly above.")
    public Object[] isSunVisible(final Context context, final Arguments args) {
        return new Object[]{level != null && level.isDay() && level.canSeeSky(getBlockPos().above()) && !level.isRaining() && !level.isThundering()};
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
        final GeolyzerEvent.Scan event = new GeolyzerEvent.Scan(this, options, bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
        fillScan(event, includeReplaceable(options));
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return new Object[]{null, "scan was canceled"};
        }
        return new Object[]{event.data};
    }

    @Callback(doc = "function(side:number[,options:table]):table -- Get some information on a directly adjacent block.")
    public Object[] analyze(final Context context, final Arguments args) {
        if (!ModSettings.allowItemStackInspection()) {
            return notEnabled();
        }
        final BlockPos target = relativeBlock(args.checkInteger(0));
        final Map<?, ?> options = args.optTable(1, Map.of());
        if (!consumeEnergy()) {
            return noEnergy();
        }
        final GeolyzerEvent.Analyze event = new GeolyzerEvent.Analyze(this, options, target);
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
            throw new IllegalArgumentException("invalid slot");
        }
        final boolean overwritten = !database.getStackInSlot(slot).isEmpty();
        database.setStackInSlot(slot, stack);
        return new Object[]{overwritten};
    }

    private boolean consumeEnergy() {
        return node() instanceof Connector connector && connector.tryChangeBuffer(-ModSettings.geolyzerScanCost());
    }

    private static Object[] noEnergy() {
        return new Object[]{null, "not enough energy"};
    }

    private static Object[] notEnabled() {
        return new Object[]{null, "not enabled in config"};
    }

    @Override
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains(TAG_NODE)) {
            node().load(nbt.getCompound(TAG_NODE));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        saveNode(nbt);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    public void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    private void fillScan(final GeolyzerEvent.Scan event, final boolean includeReplaceable) {
        if (level == null) {
            return;
        }
        int index = 0;
        for (int y = event.minY; y <= event.maxY; y++) {
            for (int z = event.minZ; z <= event.maxZ; z++) {
                for (int x = event.minX; x <= event.maxX; x++) {
                    event.data[index++] = hardness(getBlockPos().offset(x, y, z), includeReplaceable);
                }
            }
        }
    }

    private float hardness(final BlockPos target, final boolean includeReplaceable) {
        if (level == null || !level.isLoaded(target)) {
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
        if (level == null || !level.isLoaded(event.pos)) {
            return;
        }
        final BlockState state = level.getBlockState(event.pos);
        final var blockKey = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        event.data.put("name", blockKey == null ? "minecraft:air" : blockKey.toString());
        if (ModSettings.insertIdsInConverters()) {
            event.data.put("id", BuiltInRegistries.BLOCK.getId(state.getBlock()));
        }
        event.data.put("hardness", state.getDestroySpeed(level, event.pos));
        event.data.put("color", state.getMapColor(level, event.pos).col);
        final Map<String, String> properties = new HashMap<>();
        state.getValues().forEach((property, value) -> properties.put(property.getName(), value.toString()));
        event.data.put("properties", properties);
        event.data.put("metadata", 0);
    }

    private BlockPos relativeBlock(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        return getBlockPos().relative(Direction.from3DDataValue(side));
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

    private void saveNode(final CompoundTag nbt) {
        if (node() == null) {
            return;
        }

        final CompoundTag nodeTag = new CompoundTag();
        if (node().address() == null) {
            Network.joinNewNetwork(node());
            node().save(nodeTag);
            node().remove();
        } else {
            node().save(nodeTag);
        }
        nbt.put(TAG_NODE, nodeTag);
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent("geolyzer", Visibility.Network)
            .withConnector()
            .create();
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

    private record ScanBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int optionsIndex) {
        int volume() {
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        boolean outOfRange() {
            final int range = ModSettings.geolyzerRange();
            return Math.abs(minX) > range || Math.abs(maxX) > range || Math.abs(minY) > range || Math.abs(maxY) > range || Math.abs(minZ) > range || Math.abs(maxZ) > range;
        }
    }
}
