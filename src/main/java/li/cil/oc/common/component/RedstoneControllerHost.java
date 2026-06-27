package li.cil.oc.common.component;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.Direction;

public interface RedstoneControllerHost extends EnvironmentHost {
    int redstoneOutput(Direction direction);

    int redstoneInput(Direction direction);

    void setRedstoneOutput(Direction direction, int value);

    default int bundledRedstoneInput(final Direction direction, final int color) {
        return 0;
    }

    default int bundledRedstoneOutput(final Direction direction, final int color) {
        return 0;
    }

    default void setBundledRedstoneOutput(final Direction direction, final int color, final int value) {
    }

    Direction toGlobal(Direction direction);

    default int wakeThreshold() {
        return 0;
    }

    default void setWakeThreshold(final int value) {
    }
}
