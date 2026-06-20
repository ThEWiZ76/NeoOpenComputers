package li.cil.oc.common.component;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.Direction;

public interface RedstoneControllerHost extends EnvironmentHost {
    int redstoneOutput(Direction direction);

    int redstoneInput(Direction direction);

    void setRedstoneOutput(Direction direction, int value);

    Direction toGlobal(Direction direction);
}
