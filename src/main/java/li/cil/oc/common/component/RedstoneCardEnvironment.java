package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class RedstoneCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "redstone";
    private static final int COLOR_COUNT = 16;

    private final EnvironmentHost host;

    public RedstoneCardEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Communication,
            DeviceInfo.DeviceAttribute.Description, "Redstone controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Rs100-V",
            DeviceInfo.DeviceAttribute.Capacity, "16",
            DeviceInfo.DeviceAttribute.Width, "1"
        );
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Gets redstone output level for one side or all sides.")
    public Object[] getOutput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        if (args.count() == 1) {
            return new Object[]{redstone.redstoneOutput(side(redstone, args.checkInteger(0)))};
        }
        return new Object[]{valuesToMap(redstone, false)};
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Gets redstone input level for one side or all sides.")
    public Object[] getInput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        if (args.count() == 1) {
            return new Object[]{redstone.redstoneInput(side(redstone, args.checkInteger(0)))};
        }
        return new Object[]{valuesToMap(redstone, true)};
    }

    @Callback(doc = "function([side:number, ]value:number or table):number or table -- Sets redstone output and returns previous value.")
    public Object[] setOutput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final Object result;
        final boolean changed;
        if (args.count() == 1 && args.isTable(0)) {
            result = valuesToMap(redstone, false);
            changed = setOutputs(redstone, args.checkTable(0));
        } else if (args.count() == 2) {
            final Direction direction = side(redstone, args.checkInteger(0));
            final int oldValue = redstone.redstoneOutput(direction);
            final int newValue = Math.clamp(args.checkInteger(1), 0, 15);
            result = oldValue;
            changed = oldValue != newValue;
            redstone.setRedstoneOutput(direction, newValue);
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1 or 2");
        }
        final double redstoneDelay = ModSettings.redstoneDelay();
        if (changed && context != null && redstoneDelay > 0D) {
            context.pause(redstoneDelay);
        }
        return new Object[]{result};
    }

    @Callback(direct = true, doc = "function(side:number):number -- Gets comparator input on the specified side.")
    public Object[] getComparatorInput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final Level level = redstone.world();
        if (level == null) {
            return new Object[]{0};
        }
        final Direction direction = side(redstone, args.checkInteger(0));
        final BlockPos origin = BlockPos.containing(redstone.xPosition(), redstone.yPosition(), redstone.zPosition());
        final BlockPos target = origin.relative(direction);
        final BlockState state = level.getBlockState(target);
        return new Object[]{state.hasAnalogOutputSignal() ? state.getAnalogOutputSignal(level, target) : 0};
    }

    @Callback(direct = true, doc = "function():number -- Gets the current wake-up threshold.")
    public Object[] getWakeThreshold(final Context context, final Arguments args) {
        return new Object[]{redstoneHost().wakeThreshold()};
    }

    @Callback(doc = "function(threshold:number):number -- Sets the wake-up threshold and returns the previous value.")
    public Object[] setWakeThreshold(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final int oldValue = redstone.wakeThreshold();
        redstone.setWakeThreshold(args.checkInteger(0));
        return new Object[]{oldValue};
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Gets bundled redstone input.")
    public Object[] getBundledInput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final BundleKey key = bundleKey(redstone, args);
        if (key.hasColor()) {
            return new Object[]{redstone.bundledRedstoneInput(key.side(), key.color())};
        }
        if (key.hasSide()) {
            return new Object[]{colorsToMap(redstone, key.side(), true)};
        }
        return new Object[]{sidesToMap(redstone, true)};
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Gets bundled redstone output.")
    public Object[] getBundledOutput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final BundleKey key = bundleKey(redstone, args);
        if (key.hasColor()) {
            return new Object[]{redstone.bundledRedstoneOutput(key.side(), key.color())};
        }
        if (key.hasSide()) {
            return new Object[]{colorsToMap(redstone, key.side(), false)};
        }
        return new Object[]{sidesToMap(redstone, false)};
    }

    @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table -- Sets bundled redstone output and returns previous value.")
    public Object[] setBundledOutput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        final Object result;
        final boolean changed;
        if (args.count() == 3) {
            final Direction direction = side(redstone, args.checkInteger(0));
            final int color = color(args.checkInteger(1));
            final int oldValue = redstone.bundledRedstoneOutput(direction, color);
            result = oldValue;
            changed = oldValue != setBundledOutput(redstone, direction, color, args.checkInteger(2));
        } else if (args.count() == 2) {
            final Direction direction = side(redstone, args.checkInteger(0));
            result = colorsToMap(redstone, direction, false);
            changed = setBundledOutputs(redstone, direction, args.checkTable(1));
        } else if (args.count() == 1 && args.isTable(0)) {
            result = sidesToMap(redstone, false);
            changed = setBundledOutputs(redstone, args.checkTable(0));
        } else {
            throw new IllegalArgumentException("invalid number of arguments, expected 1, 2, or 3");
        }
        final double redstoneDelay = ModSettings.redstoneDelay();
        if (changed && context != null && redstoneDelay > 0D) {
            context.pause(redstoneDelay);
        }
        return new Object[]{result};
    }

    private RedstoneControllerHost redstoneHost() {
        if (host instanceof RedstoneControllerHost redstone) {
            return redstone;
        }
        throw new IllegalStateException("redstone card requires redstone controller host");
    }

    private static Direction side(final RedstoneControllerHost redstone, final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        return redstone.toGlobal(Direction.from3DDataValue(side));
    }

    private static boolean setOutputs(final RedstoneControllerHost redstone, final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number side && entry.getValue() instanceof Number value) {
                final Direction direction = side(redstone, side.intValue());
                final int oldValue = redstone.redstoneOutput(direction);
                final int newValue = Math.clamp(value.intValue(), 0, 15);
                changed |= oldValue != newValue;
                redstone.setRedstoneOutput(direction, newValue);
            }
        }
        return changed;
    }

    private static Map<Integer, Integer> valuesToMap(final RedstoneControllerHost redstone, final boolean input) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (Direction direction : Direction.values()) {
            final int side = direction.get3DDataValue();
            final Direction global = redstone.toGlobal(direction);
            result.put(side, input ? redstone.redstoneInput(global) : redstone.redstoneOutput(global));
        }
        return result;
    }

    private static int color(final int value) {
        if (value < 0 || value >= COLOR_COUNT) {
            throw new IllegalArgumentException("invalid color");
        }
        return value;
    }

    private static BundleKey bundleKey(final RedstoneControllerHost redstone, final Arguments args) {
        return switch (args.count()) {
            case 0 -> new BundleKey(null, -1);
            case 1 -> new BundleKey(side(redstone, args.checkInteger(0)), -1);
            case 2 -> new BundleKey(side(redstone, args.checkInteger(0)), color(args.checkInteger(1)));
            default -> throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
        };
    }

    private static Map<Integer, Integer> colorsToMap(final RedstoneControllerHost redstone, final Direction direction, final boolean input) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (int color = 0; color < COLOR_COUNT; color++) {
            result.put(color, input
                ? redstone.bundledRedstoneInput(direction, color)
                : redstone.bundledRedstoneOutput(direction, color));
        }
        return result;
    }

    private static Map<Integer, Map<Integer, Integer>> sidesToMap(final RedstoneControllerHost redstone, final boolean input) {
        final Map<Integer, Map<Integer, Integer>> result = new HashMap<>();
        for (Direction direction : Direction.values()) {
            result.put(direction.get3DDataValue(), colorsToMap(redstone, redstone.toGlobal(direction), input));
        }
        return result;
    }

    private static int setBundledOutput(final RedstoneControllerHost redstone, final Direction direction, final int color, final int value) {
        final int newValue = Math.clamp(value, 0, 255);
        redstone.setBundledRedstoneOutput(direction, color, newValue);
        return newValue;
    }

    private static boolean setBundledOutputs(final RedstoneControllerHost redstone, final Direction direction, final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number color && entry.getValue() instanceof Number value) {
                final int checkedColor = color(color.intValue());
                final int oldValue = redstone.bundledRedstoneOutput(direction, checkedColor);
                final int newValue = setBundledOutput(redstone, direction, checkedColor, value.intValue());
                changed |= oldValue != newValue;
            }
        }
        return changed;
    }

    private static boolean setBundledOutputs(final RedstoneControllerHost redstone, final Map<?, ?> values) {
        boolean changed = false;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() instanceof Number side && entry.getValue() instanceof Map<?, ?> colors) {
                changed |= setBundledOutputs(redstone, side(redstone, side.intValue()), colors);
            }
        }
        return changed;
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
