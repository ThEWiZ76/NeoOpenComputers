package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.HologramBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Map;

public class HologramBlockEntity extends BlockEntity implements Environment, EnvironmentHost, DeviceInfo {
    public static final int WIDTH = 48;
    public static final int HEIGHT = 32;

    private static final int VOLUME_SIZE = WIDTH * WIDTH * 2;
    private static final String TAG_NODE = "node";
    private static final String TAG_TIER = "oc:tier";
    private static final String TAG_VOLUME = "volume";
    private static final String TAG_COLORS = "colors";
    private static final String TAG_SCALE = "oc:scale";
    private static final String TAG_TRANSLATION_X = "oc:translationX";
    private static final String TAG_TRANSLATION_Y = "oc:translationY";
    private static final String TAG_TRANSLATION_Z = "oc:translationZ";
    private static final String TAG_ROTATION_ANGLE = "oc:rotationAngle";
    private static final String TAG_ROTATION_X = "oc:rotationX";
    private static final String TAG_ROTATION_Y = "oc:rotationY";
    private static final String TAG_ROTATION_Z = "oc:rotationZ";
    private static final String TAG_ROTATION_SPEED = "oc:rotationSpeed";
    private static final String TAG_ROTATION_SPEED_X = "oc:rotationSpeedX";
    private static final String TAG_ROTATION_SPEED_Y = "oc:rotationSpeedY";
    private static final String TAG_ROTATION_SPEED_Z = "oc:rotationSpeedZ";
    private static final double[] MAX_SCALE_BY_TIER = {3.0D, 4.0D};
    private static final double[] MAX_TRANSLATION_BY_TIER = {1.0D, 2.0D};
    private static final String COMPONENT_NAME = "hologram";

    private final int[] volume = new int[VOLUME_SIZE];
    private Node node;
    private int tier;
    private int[] colors;
    private double scale = 1.0D;
    private double translationX;
    private double translationY;
    private double translationZ;
    private float rotationAngle;
    private float rotationX;
    private float rotationY;
    private float rotationZ;
    private float rotationSpeed;
    private float rotationSpeedX;
    private float rotationSpeedY;
    private float rotationSpeedZ;

    public HologramBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.HOLOGRAM.get(), pos, blockState);
        OpenComputersApi.initialize();
        tier = tierFromState(blockState);
        colors = defaultColors(tier);
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
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
            DeviceInfo.DeviceAttribute.Description, "Holographic projector",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "VirtualViewer H1-" + (tier + 1),
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(WIDTH * WIDTH * HEIGHT),
            DeviceInfo.DeviceAttribute.Width, Integer.toString(colors.length)
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

    @Callback(doc = "function() -- Clears the hologram.")
    public synchronized Object[] clear(final Context context, final Arguments args) {
        Arrays.fill(volume, 0);
        setChanged();
        return null;
    }

    @Callback(direct = true, doc = "function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.")
    public synchronized Object[] get(final Context context, final Arguments args) {
        final int x = checkCoordinate(args, 0, WIDTH, "x");
        final int y = checkCoordinate(args, 1, HEIGHT, "y");
        final int z = checkCoordinate(args, 2, WIDTH, "z");
        return new Object[]{getColor(x, y, z)};
    }

    @Callback(direct = true, limit = 256, doc = "function(x:number, y:number, z:number, value:number or boolean) -- Sets the specified voxel.")
    public synchronized Object[] set(final Context context, final Arguments args) {
        final int x = checkCoordinate(args, 0, WIDTH, "x");
        final int y = checkCoordinate(args, 1, HEIGHT, "y");
        final int z = checkCoordinate(args, 2, WIDTH, "z");
        setColor(x, y, z, checkColor(args, 3));
        setChanged();
        return null;
    }

    @Callback(direct = true, limit = 128, doc = "function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills a column interval.")
    public synchronized Object[] fill(final Context context, final Arguments args) {
        final int x = checkCoordinate(args, 0, WIDTH, "x");
        final int z = checkCoordinate(args, 1, WIDTH, "z");
        final int minY;
        final int maxY;
        final int value;
        if (args.count() > 4) {
            minY = clamp(args.checkInteger(2), 1, HEIGHT);
            maxY = clamp(args.checkInteger(3), 1, HEIGHT);
            value = checkColor(args, 4);
        } else {
            minY = 1;
            maxY = clamp(args.checkInteger(2), 1, HEIGHT);
            value = checkColor(args, 3);
        }
        if (minY > maxY) {
            throw new IllegalArgumentException("interval is empty");
        }
        final int mask = (-1 >>> (31 - (maxY - minY))) << (minY - 1);
        final int index = x + z * WIDTH;
        if ((value & 1) == 0) {
            volume[index] &= ~mask;
        } else {
            volume[index] |= mask;
        }
        if (((value >>> 1) & 1) == 0) {
            volume[index + WIDTH * WIDTH] &= ~mask;
        } else {
            volume[index + WIDTH * WIDTH] |= mask;
        }
        setChanged();
        return null;
    }

    @Callback(doc = "function(data:string) -- Sets the raw voxel buffer.")
    public synchronized Object[] setRaw(final Context context, final Arguments args) {
        final byte[] data = args.checkByteArray(0);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < WIDTH; z++) {
                final int offset = z * HEIGHT + x * HEIGHT * WIDTH;
                if (data.length >= offset + HEIGHT) {
                    int lowBits = 0;
                    int highBits = 0;
                    for (int y = HEIGHT - 1; y >= 0; y--) {
                        final int color = data[offset + y] & 0xFF;
                        lowBits |= (color & 1) << y;
                        highBits |= ((color & 3) >>> 1) << y;
                    }
                    final int index = x + z * WIDTH;
                    volume[index] = lowBits;
                    volume[index + WIDTH * WIDTH] = highBits;
                }
            }
        }
        context.pause(0.5D);
        setChanged();
        return null;
    }

    @Callback(doc = "function(x:number, z:number, sx:number, sz:number, tx:number, tz:number) -- Copies an area of columns.")
    public synchronized Object[] copy(final Context context, final Arguments args) {
        final int x = checkCoordinate(args, 0, WIDTH, "x");
        final int z = checkCoordinate(args, 1, WIDTH, "z");
        final int width = args.checkInteger(2);
        final int depth = args.checkInteger(3);
        final int tx = args.checkInteger(4);
        final int tz = args.checkInteger(5);
        if (width <= 0 || depth <= 0 || (tx == 0 && tz == 0)) {
            return null;
        }
        final int[] previous = volume.clone();
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                final int sourceX = x + dx;
                final int sourceZ = z + dz;
                final int targetX = sourceX + tx;
                final int targetZ = sourceZ + tz;
                if (sourceX < 0 || sourceX >= WIDTH || sourceZ < 0 || sourceZ >= WIDTH ||
                    targetX < 0 || targetX >= WIDTH || targetZ < 0 || targetZ >= WIDTH) {
                    continue;
                }
                final int sourceIndex = sourceX + sourceZ * WIDTH;
                final int targetIndex = targetX + targetZ * WIDTH;
                volume[targetIndex] = previous[sourceIndex];
                volume[targetIndex + WIDTH * WIDTH] = previous[sourceIndex + WIDTH * WIDTH];
            }
        }
        context.pause(Math.max(0D, (width * depth) / (double) (WIDTH * WIDTH) - 0.25D));
        setChanged();
        return null;
    }

    @Callback(direct = true, doc = "function():number -- Returns the render scale.")
    public Object[] getScale(final Context context, final Arguments args) {
        return new Object[]{scale};
    }

    @Callback(doc = "function(value:number) -- Sets the render scale.")
    public Object[] setScale(final Context context, final Arguments args) {
        scale = Math.max(0.333333D, Math.min(maxScale(), args.checkDouble(0)));
        setChanged();
        return null;
    }

    @Callback(direct = true, doc = "function():number, number, number -- Returns relative projection offsets.")
    public Object[] getTranslation(final Context context, final Arguments args) {
        return new Object[]{translationX, translationY, translationZ};
    }

    @Callback(doc = "function(tx:number, ty:number, tz:number) -- Sets relative projection offsets.")
    public Object[] setTranslation(final Context context, final Arguments args) {
        final double maxTranslation = maxTranslation();
        translationX = Math.max(-maxTranslation, Math.min(maxTranslation, args.checkDouble(0)));
        translationY = Math.max(0D, Math.min(maxTranslation * 2D, args.checkDouble(1)));
        translationZ = Math.max(-maxTranslation, Math.min(maxTranslation, args.checkDouble(2)));
        setChanged();
        return null;
    }

    @Callback(direct = true, doc = "function():number -- Returns supported color depth.")
    public Object[] maxDepth(final Context context, final Arguments args) {
        return new Object[]{tier + 1};
    }

    @Callback(doc = "function(index:number):number -- Gets a palette color.")
    public Object[] getPaletteColor(final Context context, final Arguments args) {
        return new Object[]{colors[checkPaletteIndex(args.checkInteger(0))]};
    }

    @Callback(doc = "function(index:number, value:number):number -- Sets a palette color and returns the old value.")
    public Object[] setPaletteColor(final Context context, final Arguments args) {
        final int index = checkPaletteIndex(args.checkInteger(0));
        final int oldValue = colors[index];
        colors[index] = args.checkInteger(1) & 0xFFFFFF;
        setChanged();
        return new Object[]{oldValue};
    }

    @Callback(doc = "function(angle:number, x:number, y:number, z:number):boolean -- Sets the tier-2 base rotation.")
    public Object[] setRotation(final Context context, final Arguments args) {
        if (tier == 0) {
            return new Object[]{null, "not supported"};
        }
        rotationAngle = (float) (args.checkDouble(0) % 360D);
        rotationX = (float) args.checkDouble(1);
        rotationY = (float) args.checkDouble(2);
        rotationZ = (float) args.checkDouble(3);
        setChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function(speed:number, x:number, y:number, z:number):boolean -- Sets the tier-2 rotation speed.")
    public Object[] setRotationSpeed(final Context context, final Arguments args) {
        if (tier == 0) {
            return new Object[]{null, "not supported"};
        }
        rotationSpeed = (float) Math.max(-1440D, Math.min(1440D, args.checkDouble(0)));
        rotationSpeedX = (float) args.checkDouble(1);
        rotationSpeedY = (float) args.checkDouble(2);
        rotationSpeedZ = (float) args.checkDouble(3);
        setChanged();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number, number, number -- Gets the x/y/z dimensions.")
    public Object[] getDimensions(final Context context, final Arguments args) {
        return new Object[]{WIDTH, HEIGHT, WIDTH};
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
        if (tag.contains(TAG_TIER)) {
            tier = clamp(tag.getByte(TAG_TIER), 0, 1);
            colors = defaultColors(tier);
        }
        copyInto(tag.getIntArray(TAG_VOLUME), volume);
        copyInto(tag.getIntArray(TAG_COLORS), colors);
        if (tag.contains(TAG_SCALE)) {
            scale = Math.max(0.333333D, Math.min(maxScale(), tag.getDouble(TAG_SCALE)));
        }
        translationX = tag.getDouble(TAG_TRANSLATION_X);
        translationY = tag.getDouble(TAG_TRANSLATION_Y);
        translationZ = tag.getDouble(TAG_TRANSLATION_Z);
        rotationAngle = tag.getFloat(TAG_ROTATION_ANGLE);
        rotationX = tag.getFloat(TAG_ROTATION_X);
        rotationY = tag.getFloat(TAG_ROTATION_Y);
        rotationZ = tag.getFloat(TAG_ROTATION_Z);
        rotationSpeed = tag.getFloat(TAG_ROTATION_SPEED);
        rotationSpeedX = tag.getFloat(TAG_ROTATION_SPEED_X);
        rotationSpeedY = tag.getFloat(TAG_ROTATION_SPEED_Y);
        rotationSpeedZ = tag.getFloat(TAG_ROTATION_SPEED_Z);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveNode(tag);
        tag.putByte(TAG_TIER, (byte) tier);
        tag.putIntArray(TAG_VOLUME, volume);
        tag.putIntArray(TAG_COLORS, colors);
        tag.putDouble(TAG_SCALE, scale);
        tag.putDouble(TAG_TRANSLATION_X, translationX);
        tag.putDouble(TAG_TRANSLATION_Y, translationY);
        tag.putDouble(TAG_TRANSLATION_Z, translationZ);
        tag.putFloat(TAG_ROTATION_ANGLE, rotationAngle);
        tag.putFloat(TAG_ROTATION_X, rotationX);
        tag.putFloat(TAG_ROTATION_Y, rotationY);
        tag.putFloat(TAG_ROTATION_Z, rotationZ);
        tag.putFloat(TAG_ROTATION_SPEED, rotationSpeed);
        tag.putFloat(TAG_ROTATION_SPEED_X, rotationSpeedX);
        tag.putFloat(TAG_ROTATION_SPEED_Y, rotationSpeedY);
        tag.putFloat(TAG_ROTATION_SPEED_Z, rotationSpeedZ);
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

    private int getColor(final int x, final int y, final int z) {
        final int lowBit = (volume[x + z * WIDTH] >>> y) & 1;
        final int highBit = (volume[x + z * WIDTH + WIDTH * WIDTH] >>> y) & 1;
        return lowBit | (highBit << 1);
    }

    private void setColor(final int x, final int y, final int z, final int value) {
        final int index = x + z * WIDTH;
        final int bit = 1 << y;
        if ((value & 1) == 0) {
            volume[index] &= ~bit;
        } else {
            volume[index] |= bit;
        }
        if (((value >>> 1) & 1) == 0) {
            volume[index + WIDTH * WIDTH] &= ~bit;
        } else {
            volume[index + WIDTH * WIDTH] |= bit;
        }
    }

    private void saveNode(final CompoundTag tag) {
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
        tag.put(TAG_NODE, nodeTag);
    }

    private double maxScale() {
        return MAX_SCALE_BY_TIER[tier];
    }

    private double maxTranslation() {
        return MAX_TRANSLATION_BY_TIER[tier];
    }

    private int checkColor(final Arguments args, final int index) {
        final int value = args.isBoolean(index) ? (args.checkBoolean(index) ? 1 : 0) : args.checkInteger(index);
        if (value < 0 || value > colors.length) {
            throw new IllegalArgumentException("invalid value");
        }
        return value;
    }

    private int checkPaletteIndex(final int value) {
        final int index = value - 1;
        if (index < 0 || index >= colors.length) {
            throw new ArrayIndexOutOfBoundsException("palette");
        }
        return index;
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent(COMPONENT_NAME, Visibility.Network)
            .withConnector()
            .create();
    }

    private static int checkCoordinate(final Arguments args, final int index, final int limit, final String name) {
        final int value = args.checkInteger(index) - 1;
        if (value < 0 || value >= limit) {
            throw new ArrayIndexOutOfBoundsException(name);
        }
        return value;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int tierFromState(final BlockState state) {
        return state.getBlock() instanceof HologramBlock hologram ? hologram.tier() : 0;
    }

    private static int[] defaultColors(final int tier) {
        return tier == 0 ? new int[]{0x00FF00} : new int[]{0x0000FF, 0x00FF00, 0xFF0000};
    }

    private static void copyInto(final int[] source, final int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }
}
