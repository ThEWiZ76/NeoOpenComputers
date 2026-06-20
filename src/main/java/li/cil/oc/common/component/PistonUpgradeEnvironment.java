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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.Map;

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
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
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
        final BlockPos targetPos = extending ? sourcePos.relative(direction) : hostPos.relative(direction);
        final BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir()) {
            return new Object[]{false, "move failed"};
        }
        if (!level.getBlockState(targetPos).isAir()) {
            return new Object[]{false, "path is obstructed"};
        }
        if (!isMovable(sourceState)) {
            return new Object[]{false, "move failed"};
        }

        level.setBlock(targetPos, sourceState, 3);
        level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), 3);
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
        return rotatable.toGlobal(Direction.from3DDataValue(arguments.checkInteger(0)));
    }

    private BlockPos hostPosition() {
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }

    private static boolean isMovable(final BlockState state) {
        final PushReaction reaction = state.getPistonPushReaction();
        return reaction == PushReaction.NORMAL || reaction == PushReaction.PUSH_ONLY;
    }
}
