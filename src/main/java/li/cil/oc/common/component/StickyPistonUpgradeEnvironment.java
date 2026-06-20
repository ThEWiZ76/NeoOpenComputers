package li.cil.oc.common.component;

import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;

public class StickyPistonUpgradeEnvironment extends PistonUpgradeEnvironment {
    public StickyPistonUpgradeEnvironment(final EnvironmentHost host, final Rotatable rotatable) {
        super(host, rotatable, true);
    }

    @Callback(doc = "function([side:number]):boolean -- Tries to reach out to the side given (default front) and pull a block similar to a vanilla sticky piston.")
    public Object[] pull(final Context context, final Arguments arguments) {
        return move(context, arguments, false);
    }
}
