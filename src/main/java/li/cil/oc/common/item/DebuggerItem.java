package li.cil.oc.common.item;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Arrays;

public final class DebuggerItem extends Item implements Environment {
    private Node node;

    public DebuggerItem(final Properties properties) {
        super(properties);
    }

    @Override
    public Node node() {
        if (node == null) node = Network.newNode(this, Visibility.Network).create();
        return node;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer) || context.getPlayer() instanceof FakePlayer) return InteractionResult.PASS;
        final var target = context.getLevel().getBlockEntity(context.getClickedPos());
        final Node selected;
        if (target instanceof SidedEnvironment sided) selected = sided.sidedNode(context.getClickedFace());
        else if (target instanceof Environment environment) selected = environment.node();
        else {
            node().remove();
            return InteractionResult.CONSUME;
        }
        node().remove();
        Network.joinNewNetwork(node());
        if (selected != null) node().connect(selected);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onConnect(final Node connected) {
        NeoOpenComputers.LOGGER.info("[NETWORK DEBUGGER] New node in network: {}", nodeInfo(connected));
    }

    @Override
    public void onDisconnect(final Node disconnected) {
        NeoOpenComputers.LOGGER.info("[NETWORK DEBUGGER] Node removed from network: {}", nodeInfo(disconnected));
    }

    @Override
    public void onMessage(final Message message) {
        NeoOpenComputers.LOGGER.info("[NETWORK DEBUGGER] Received message: {name = {}, source = {}, data = {}}.",
            message.name(), nodeInfo(message.source()), Arrays.toString(message.data()));
    }

    private static String nodeInfo(final Node node) {
        final var info = new StringBuilder("{address = ").append(node.address()).append(", reachability = ").append(node.reachability().name());
        if (node instanceof Component component) {
            info.append(", type = component, name = ").append(component.name()).append(", visibility = ").append(component.visibility().name());
        }
        if (node instanceof Connector connector) {
            info.append(", type = connector, buffer = ").append(connector.localBuffer()).append(", bufferSize = ").append(connector.localBufferSize());
        }
        return info.append('}').toString();
    }

    public static void onServerStopped(final ServerStoppedEvent event) {
        final var debugger = ModItems.DEBUGGER.get();
        if (debugger.node != null) {
            debugger.node.remove();
            debugger.node = null;
        }
    }
}
