package li.cil.oc.api.network;

import net.minecraft.world.level.Level;

public interface EnvironmentHost {
    Level world();

    double xPosition();

    double yPosition();

    double zPosition();

    void markChanged();
}
