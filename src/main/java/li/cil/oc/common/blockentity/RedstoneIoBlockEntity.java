package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class RedstoneIoBlockEntity extends BlockEntity implements Environment, RedstoneControllerHost, DeviceInfo {
    private static final String TAG_NODE = "node";
    private static final String TAG_OUTPUTS = "oc:redstoneOutputs";
    private static final String TAG_BUNDLED_OUTPUTS = "oc:bundledRedstoneOutputs";
    private static final String TAG_WAKE_THRESHOLD = "oc:wakeThreshold";
    private static final int COLOR_COUNT = 16;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Communication,
        DeviceInfo.DeviceAttribute.Description, "Redstone controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Rs100-V",
        DeviceInfo.DeviceAttribute.Capacity, "16",
        DeviceInfo.DeviceAttribute.Width, "1"
    );

    private final int[] outputs = new int[6];
    private final int[] inputs = new int[6];
    private final int[][] bundledOutputs = new int[6][COLOR_COUNT];
    private Node node;
    private int wakeThreshold;

    public RedstoneIoBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.REDSTONE_IO.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RedstoneIoBlockEntity blockEntity) {
        blockEntity.updateRedstoneInputs();
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
        return DEVICE_INFO;
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

    @Override
    public int redstoneOutput(final Direction direction) {
        return outputs[direction.get3DDataValue()];
    }

    @Override
    public int redstoneInput(final Direction direction) {
        return inputs[direction.get3DDataValue()];
    }

    @Override
    public void setRedstoneOutput(final Direction direction, final int value) {
        final int index = direction.get3DDataValue();
        final int clamped = Math.clamp(value, 0, 15);
        if (outputs[index] == clamped) {
            return;
        }
        outputs[index] = clamped;
        setChanged();
        if (level != null) {
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            level.updateNeighborsAt(getBlockPos().relative(direction), getBlockState().getBlock());
        }
    }

    @Override
    public int wakeThreshold() {
        return wakeThreshold;
    }

    @Override
    public void setWakeThreshold(final int value) {
        wakeThreshold = value;
        setChanged();
    }

    @Override
    public Direction toGlobal(final Direction direction) {
        return direction;
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get redstone input for one side or all sides.")
    public Object[] getInput(final Context context, final Arguments args) {
        if (args.count() == 1) {
            return new Object[]{redstoneInput(side(args.checkInteger(0)))};
        }
        return new Object[]{valuesToMap(inputs)};
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get redstone output for one side or all sides.")
    public Object[] getOutput(final Context context, final Arguments args) {
        if (args.count() == 1) {
            return new Object[]{redstoneOutput(side(args.checkInteger(0)))};
        }
        return new Object[]{valuesToMap(outputs)};
    }

    @Callback(doc = "function([side:number, ]value:number or table):number or table -- Set redstone output and return previous value.")
    public Object[] setOutput(final Context context, final Arguments args) {
        final Object result;
        final boolean changed;
        if (args.count() == 1 && args.isTable(0)) {
            result = valuesToMap(outputs);
            changed = setOutputs(args.checkTable(0));
        } else if (args.count() == 2) {
            final Direction direction = side(args.checkInteger(0));
            final int oldValue = redstoneOutput(direction);
            final int newValue = Math.clamp(args.checkInteger(1), 0, 15);
            result = oldValue;
            changed = oldValue != newValue;
            setRedstoneOutput(direction, newValue);
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1 or 2");
        }
        final double redstoneDelay = ModSettings.redstoneDelay();
        if (changed && context != null && redstoneDelay > 0D) {
            context.pause(redstoneDelay);
        }
        return new Object[]{result};
    }

    @Callback(direct = true, doc = "function(side:number):number -- Get the comparator input on the specified side.")
    public Object[] getComparatorInput(final Context context, final Arguments args) {
        if (level == null) {
            return new Object[]{0};
        }
        final Direction direction = side(args.checkInteger(0));
        final BlockPos target = getBlockPos().relative(direction);
        final BlockState state = level.getBlockState(target);
        return new Object[]{state.hasAnalogOutputSignal() ? state.getAnalogOutputSignal(level, target) : 0};
    }

    @Callback(direct = true, doc = "function():number -- Gets the current wake-up threshold.")
    public Object[] getWakeThreshold(final Context context, final Arguments args) {
        return new Object[]{wakeThreshold};
    }

    @Callback(doc = "function(threshold:number):number -- Sets the wake-up threshold and returns the previous value.")
    public Object[] setWakeThreshold(final Context context, final Arguments args) {
        final int oldValue = wakeThreshold;
        setWakeThreshold(args.checkInteger(0));
        return new Object[]{oldValue};
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Get bundled redstone input.")
    public Object[] getBundledInput(final Context context, final Arguments args) {
        final BundleKey key = bundleKey(args);
        if (key.hasColor()) {
            return new Object[]{0};
        }
        if (key.hasSide()) {
            return new Object[]{colorsToMap(new int[COLOR_COUNT])};
        }
        final Map<Integer, Map<Integer, Integer>> result = new HashMap<>();
        for (Direction direction : Direction.values()) {
            result.put(direction.get3DDataValue(), colorsToMap(new int[COLOR_COUNT]));
        }
        return new Object[]{result};
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Get bundled redstone output.")
    public Object[] getBundledOutput(final Context context, final Arguments args) {
        final BundleKey key = bundleKey(args);
        if (key.hasColor()) {
            return new Object[]{bundledOutput(key.side(), key.color())};
        }
        if (key.hasSide()) {
            return new Object[]{colorsToMap(bundledOutputs[key.side().get3DDataValue()])};
        }
        return new Object[]{sidesToMap(bundledOutputs)};
    }

    @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table -- Set bundled redstone output and return previous value.")
    public Object[] setBundledOutput(final Context context, final Arguments args) {
        final Object result;
        final boolean changed;
        if (args.count() == 3) {
            final Direction direction = side(args.checkInteger(0));
            final int color = color(args.checkInteger(1));
            final int oldValue = bundledOutput(direction, color);
            result = oldValue;
            changed = oldValue != setBundledOutput(direction, color, args.checkInteger(2));
        } else if (args.count() == 2) {
            final Direction direction = side(args.checkInteger(0));
            result = colorsToMap(bundledOutputs[direction.get3DDataValue()]);
            changed = setBundledOutputs(direction, args.checkTable(1));
        } else if (args.count() == 1 && args.isTable(0)) {
            result = sidesToMap(bundledOutputs);
            changed = setBundledOutputs(args.checkTable(0));
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1, 2, or 3");
        }
        final double redstoneDelay = ModSettings.redstoneDelay();
        if (changed && context != null && redstoneDelay > 0D) {
            context.pause(redstoneDelay);
        }
        return new Object[]{result};
    }

    public void updateRedstoneInputs() {
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            final int index = direction.get3DDataValue();
            final int oldValue = inputs[index];
            final int newValue = Math.clamp(level.getSignal(getBlockPos().relative(direction), direction), 0, 15);
            if (oldValue != newValue) {
                inputs[index] = newValue;
                if (node() != null) {
                    node().sendToReachable("computer.signal", "redstone_changed", index, oldValue, newValue);
                    if (oldValue < wakeThreshold && newValue >= wakeThreshold) {
                        node().sendToReachable("computer.start");
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
        wakeThreshold = tag.getInt(TAG_WAKE_THRESHOLD);
        final int[] saved = tag.getIntArray(TAG_OUTPUTS);
        for (int index = 0; index < outputs.length; index++) {
            outputs[index] = index < saved.length ? Math.clamp(saved[index], 0, 15) : 0;
        }
        loadBundledOutputs(tag.getIntArray(TAG_BUNDLED_OUTPUTS));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveNode(tag);
        tag.putInt(TAG_WAKE_THRESHOLD, wakeThreshold);
        tag.putIntArray(TAG_OUTPUTS, outputs);
        tag.putIntArray(TAG_BUNDLED_OUTPUTS, saveBundledOutputs());
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

    private boolean setOutputs(final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number side && entry.getValue() instanceof Number value) {
                final Direction direction = side(side.intValue());
                final int oldValue = redstoneOutput(direction);
                final int newValue = Math.clamp(value.intValue(), 0, 15);
                changed |= oldValue != newValue;
                setRedstoneOutput(direction, newValue);
            }
        }
        return changed;
    }

    private int bundledOutput(final Direction direction, final int color) {
        return bundledOutputs[direction.get3DDataValue()][color];
    }

    private int setBundledOutput(final Direction direction, final int color, final int value) {
        final int side = direction.get3DDataValue();
        final int oldValue = bundledOutputs[side][color];
        final int newValue = Math.clamp(value, 0, 255);
        if (oldValue != newValue) {
            bundledOutputs[side][color] = newValue;
            setChanged();
        }
        return newValue;
    }

    private boolean setBundledOutputs(final Direction direction, final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number color && entry.getValue() instanceof Number value) {
                final int checkedColor = color(color.intValue());
                final int oldValue = bundledOutput(direction, checkedColor);
                final int newValue = setBundledOutput(direction, checkedColor, value.intValue());
                changed |= oldValue != newValue;
            }
        }
        return changed;
    }

    private boolean setBundledOutputs(final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number side && entry.getValue() instanceof Map<?, ?> colors) {
                changed |= setBundledOutputs(side(side.intValue()), colors);
            }
        }
        return changed;
    }

    private int[] saveBundledOutputs() {
        final int[] saved = new int[outputs.length * COLOR_COUNT];
        for (Direction direction : Direction.values()) {
            final int side = direction.get3DDataValue();
            System.arraycopy(bundledOutputs[side], 0, saved, side * COLOR_COUNT, COLOR_COUNT);
        }
        return saved;
    }

    private void loadBundledOutputs(final int[] saved) {
        for (Direction direction : Direction.values()) {
            final int side = direction.get3DDataValue();
            for (int color = 0; color < COLOR_COUNT; color++) {
                final int index = side * COLOR_COUNT + color;
                bundledOutputs[side][color] = index < saved.length ? Math.clamp(saved[index], 0, 255) : 0;
            }
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

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent("redstone", Visibility.Network)
            .withConnector()
            .create();
    }

    private static Direction side(final int value) {
        if (value < 0 || value > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        return Direction.from3DDataValue(value);
    }

    private static int color(final int value) {
        if (value < 0 || value >= COLOR_COUNT) {
            throw new IllegalArgumentException("invalid color");
        }
        return value;
    }

    private static BundleKey bundleKey(final Arguments args) {
        return switch (args.count()) {
            case 0 -> new BundleKey(null, -1);
            case 1 -> new BundleKey(side(args.checkInteger(0)), -1);
            case 2 -> new BundleKey(side(args.checkInteger(0)), color(args.checkInteger(1)));
            default -> throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
        };
    }

    private static Map<Integer, Integer> valuesToMap(final int[] values) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (Direction direction : Direction.values()) {
            result.put(direction.get3DDataValue(), values[direction.get3DDataValue()]);
        }
        return result;
    }

    private static Map<Integer, Integer> colorsToMap(final int[] values) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (int color = 0; color < COLOR_COUNT; color++) {
            result.put(color, color < values.length ? values[color] : 0);
        }
        return result;
    }

    private static Map<Integer, Map<Integer, Integer>> sidesToMap(final int[][] values) {
        final Map<Integer, Map<Integer, Integer>> result = new HashMap<>();
        for (Direction direction : Direction.values()) {
            result.put(direction.get3DDataValue(), colorsToMap(values[direction.get3DDataValue()]));
        }
        return result;
    }

    private record BundleKey(Direction side, int color) {
        private boolean hasSide() {
            return side != null;
        }

        private boolean hasColor() {
            return side != null && color >= 0;
        }
    }
}
