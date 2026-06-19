package li.cil.oc.api.network;

import net.minecraft.core.Direction;

public interface SidedComponent {
    boolean canConnectNode(Direction side);
}
