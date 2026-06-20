package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.Direction;

import java.util.Map;

public class RedstoneCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "redstone";

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
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Redstone controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Redstone Card"
        );
    }

    @Callback(direct = true, doc = "function(side:number):number -- Gets redstone output level on the specified side.")
    public Object[] getOutput(final Context context, final Arguments args) {
        return new Object[]{computerCaseHost().redstoneOutput(direction(args.checkInteger(0)))};
    }

    @Callback(doc = "function(side:number, value:number):number -- Sets redstone output level and returns the previous value.")
    public Object[] setOutput(final Context context, final Arguments args) {
        final Direction direction = direction(args.checkInteger(0));
        final ComputerCaseBlockEntity computerCase = computerCaseHost();
        final int oldValue = computerCase.redstoneOutput(direction);
        computerCase.setRedstoneOutput(direction, args.checkInteger(1));
        return new Object[]{oldValue};
    }

    private ComputerCaseBlockEntity computerCaseHost() {
        if (host instanceof ComputerCaseBlockEntity computerCase) {
            return computerCase;
        }
        throw new IllegalStateException("redstone card requires computer case host");
    }

    private static Direction direction(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        return Direction.from3DDataValue(side);
    }
}
