package li.cil.oc.api.network;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;

public interface ManagedPeripheral {
    String[] methods();

    Object[] invoke(String method, Context context, Arguments args) throws Exception;
}
