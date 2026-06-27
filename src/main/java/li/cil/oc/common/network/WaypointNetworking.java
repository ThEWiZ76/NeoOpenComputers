package li.cil.oc.common.network;

import li.cil.oc.common.menu.WaypointMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class WaypointNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                WaypointLabelPayload.TYPE,
                WaypointLabelPayload.STREAM_CODEC,
                WaypointNetworking::handleWaypointLabel);
    }

    static boolean applyWaypointLabel(final AbstractContainerMenu containerMenu, final WaypointLabelPayload payload) {
        if (!(containerMenu instanceof WaypointMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        menu.setLabel(payload.label());
        return true;
    }

    static boolean applyWaypointLabel(final Player player, final AbstractContainerMenu containerMenu, final WaypointLabelPayload payload) {
        if (player == null || !(containerMenu instanceof WaypointMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!menu.stillValid(player)) {
            return false;
        }
        return applyWaypointLabel(containerMenu, payload);
    }

    private static void handleWaypointLabel(final WaypointLabelPayload payload, final IPayloadContext context) {
        applyWaypointLabel(context.player(), context.player().containerMenu, payload);
    }

    private WaypointNetworking() {
    }
}
