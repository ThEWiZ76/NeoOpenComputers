package li.cil.oc.common.network;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.menu.RackMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RackNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToServer(
                RackControlPayload.TYPE,
                RackControlPayload.STREAM_CODEC,
                RackNetworking::handleRackControl);
    }

    static boolean applyRackControl(final AbstractContainerMenu containerMenu, final RackControlPayload payload) {
        if (!(containerMenu instanceof RackMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.rackInventory() instanceof RackBlockEntity rack) || payload.slot() < 0 || payload.slot() >= RackBlockEntity.CONTAINER_SIZE) {
            return false;
        }
        final RackMountable mountable = rack.getMountable(payload.slot());
        return mountable instanceof ServerRackMountableEnvironment server && server.controlPower(payload.action());
    }

    private static void handleRackControl(final RackControlPayload payload, final IPayloadContext context) {
        applyRackControl(context.player().containerMenu, payload);
    }

    private RackNetworking() {
    }
}
