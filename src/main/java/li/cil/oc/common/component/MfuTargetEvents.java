package li.cil.oc.common.component;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class MfuTargetEvents {
    private MfuTargetEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MfuTargetEvents::onNeighborNotify);
    }

    private static void onNeighborNotify(final BlockEvent.NeighborNotifyEvent event) {
        MfuEnvironment.refreshTargetChanged(event.getLevel(), event.getPos());
    }
}
