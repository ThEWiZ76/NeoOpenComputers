package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PistonUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "piston";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Piston upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Displacer II+"
    );

    private final EnvironmentHost host;
    private final Rotatable rotatable;
    private final boolean sticky;

    public PistonUpgradeEnvironment(final EnvironmentHost host, final Rotatable rotatable) {
        this(host, rotatable, false);
    }

    protected PistonUpgradeEnvironment(final EnvironmentHost host, final Rotatable rotatable, final boolean sticky) {
        this.host = host;
        this.rotatable = rotatable;
        this.sticky = sticky;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function():boolean -- Returns true if the piston is sticky, i.e. it can also pull.")
    public Object[] isSticky(final Context context, final Arguments arguments) {
        return new Object[]{sticky};
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to push the block on the specified side of the container of the upgrade. Defaults to front.")
    public Object[] push(final Context context, final Arguments arguments) {
        return move(context, arguments, true);
    }

    protected Object[] move(final Context context, final Arguments arguments, final boolean extending) {
        final Level level = host.world();
        if (level == null) {
            return new Object[]{false, "move failed"};
        }

        final Direction direction = direction(arguments);
        final BlockPos hostPos = hostPosition();
        final BlockPos sourcePos = extending ? hostPos.relative(direction) : hostPos.relative(direction).relative(direction);
        final BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir()) {
            return new Object[]{false, "move failed"};
        }
        final Direction moveDirection = extending ? direction : direction.getOpposite();
        if (!PistonBaseBlock.isPushable(sourceState, level, sourcePos, moveDirection, extending, direction)) {
            return new Object[]{false, "move failed"};
        }
        final PistonStructureResolver resolver = new PistonStructureResolver(level, hostPos, direction, extending);
        if (!resolver.resolve()) {
            return new Object[]{false, "path is obstructed"};
        }

        moveBlocks(level, resolver, moveDirection);
        host.markChanged();
        if (context != null) {
            context.pause(1D / 20D);
        }
        return new Object[]{true};
    }

    private Direction direction(final Arguments arguments) {
        if (arguments.count() <= 0) {
            return rotatable.facing();
        }
        final int side = arguments.checkInteger(0);
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        final Direction localSide = Direction.from3DDataValue(side);
        if (localSide != Direction.SOUTH && localSide != Direction.UP && localSide != Direction.DOWN) {
            throw new IllegalArgumentException("unsupported side");
        }
        return rotatable.toGlobal(localSide);
    }

    private BlockPos hostPosition() {
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }

    private static void moveBlocks(final Level level, final PistonStructureResolver resolver, final Direction moveDirection) {
        final List<BlockPos> toDestroy = resolver.getToDestroy();
        for (int index = toDestroy.size() - 1; index >= 0; index--) {
            level.destroyBlock(toDestroy.get(index), true);
        }

        final List<BlockPos> toPush = resolver.getToPush();
        final BlockState[] states = new BlockState[toPush.size()];
        final Set<BlockPos> targets = new HashSet<>();
        for (int index = 0; index < toPush.size(); index++) {
            final BlockPos source = toPush.get(index);
            states[index] = level.getBlockState(source);
            targets.add(source.relative(moveDirection));
        }
        for (int index = toPush.size() - 1; index >= 0; index--) {
            level.setBlock(toPush.get(index).relative(moveDirection), states[index], 3);
        }
        for (final BlockPos source : toPush) {
            if (!targets.contains(source)) {
                level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
