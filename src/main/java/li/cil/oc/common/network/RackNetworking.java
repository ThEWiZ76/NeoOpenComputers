package li.cil.oc.common.network;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
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
                RackNetworking::handleRackControl)
            .playToServer(
                RackOpenServerPayload.TYPE,
                RackOpenServerPayload.STREAM_CODEC,
                RackNetworking::handleRackOpenServer)
            .playToServer(
                ServerRackControlPayload.TYPE,
                ServerRackControlPayload.STREAM_CODEC,
                RackNetworking::handleServerRackControl);
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

    static boolean applyRackOpenServer(final Player player, final AbstractContainerMenu containerMenu, final RackOpenServerPayload payload) {
        if (player == null || !(containerMenu instanceof RackMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        if (!(menu.rackInventory() instanceof RackBlockEntity rack) || payload.slot() < 0 || payload.slot() >= RackBlockEntity.CONTAINER_SIZE) {
            return false;
        }
        final RackMountable mountable = rack.getMountable(payload.slot());
        if (!(mountable instanceof ServerRackMountableEnvironment server)) {
            return false;
        }

        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, menuPlayer) -> new ServerRackMenu(containerId, playerInventory, server),
            Component.translatable("gui.neoopencomputers.server_rack")));
        return true;
    }

    static boolean applyServerRackControl(final AbstractContainerMenu containerMenu, final ServerRackControlPayload payload) {
        if (!(containerMenu instanceof ServerRackMenu menu) || menu.containerId != payload.containerId()) {
            return false;
        }
        return menu.serverInventory() instanceof ServerRackMountableEnvironment server && server.controlPower(payload.action());
    }

    private static void handleRackControl(final RackControlPayload payload, final IPayloadContext context) {
        applyRackControl(context.player().containerMenu, payload);
    }

    private static void handleRackOpenServer(final RackOpenServerPayload payload, final IPayloadContext context) {
        applyRackOpenServer(context.player(), context.player().containerMenu, payload);
    }

    private static void handleServerRackControl(final ServerRackControlPayload payload, final IPayloadContext context) {
        applyServerRackControl(context.player().containerMenu, payload);
    }

    private RackNetworking() {
    }
}
