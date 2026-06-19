package li.cil.oc.api.internal;

import net.minecraft.core.Direction;

public interface Rotatable {
    Direction facing();

    Direction toGlobal(Direction value);

    Direction toLocal(Direction value);
}
