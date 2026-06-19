package li.cil.oc.api.network;

import net.minecraft.core.Direction;

public interface SidedEnvironment {
    Node sidedNode(Direction side);

    boolean canConnect(Direction side);
}
