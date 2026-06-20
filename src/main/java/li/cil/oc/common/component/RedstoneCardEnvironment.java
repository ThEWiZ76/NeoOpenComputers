package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class RedstoneCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "redstone";
    private static final double REDSTONE_DELAY = 0.1D;

    private final EnvironmentHost host;

    public RedstoneCardEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Communication,
            DeviceInfo.DeviceAttribute.Description, "Redstone controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Rs100-V",
            DeviceInfo.DeviceAttribute.Capacity, "16",
            DeviceInfo.DeviceAttribute.Width, "1"
        );
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Gets redstone output level for one side or all sides.")
    public Object[] getOutput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        if (args.count() == 0) {
            return new Object[]{valuesToMap(redstone, false)};
        }
        return new Object[]{redstone.redstoneOutput(side(redstone, args.checkInteger(0)))};
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Gets redstone input level for one side or all sides.")
    public Object[] getInput(final Context context, final Arguments args) {
        final RedstoneControllerHost redstone = redstoneHost();
        if (args.count() == 0) {
            return new Object[]{valuesToMap(redstone, true)};
        }
        return new Object[]{redstone.redstoneInput(side(redstone, args.checkInteger(0)))};
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
        if (changed && context != null && REDSTONE_DELAY > 0D) {
            context.pause(REDSTONE_DELAY);
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
}
