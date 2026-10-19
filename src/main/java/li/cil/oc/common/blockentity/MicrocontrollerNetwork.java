package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;

/** Separate physical plug networks and the microcontroller's private machine network. */
final class MicrocontrollerNetwork {
    private static final String PLUGS = "oc:plugs";
    private static final String CONTROLS = "oc:componentNodes";
    private static final String SNOOPER = "oc:snooper";
    private static final String OUTPUTS = "oc:outputs";
    private static final String MESSAGE = "network.message";

    private final MicrocontrollerBlockEntity host;
    private final Plug[] plugs = new Plug[6];
    private final Component[] controls = new Component[6];
    private final boolean[] outputs = new boolean[6];
    private final ComponentConnector snooper;
    private boolean suspended;
    private boolean refreshing;

    MicrocontrollerNetwork(final MicrocontrollerBlockEntity host) {
        this.host = host;
        Arrays.fill(outputs, true);
        snooper = Network.newNode(host, Visibility.Network).withComponent("microcontroller")
            .withConnector(ModSettings.microcontrollerBuffer()).create();
        for (final Direction side : Direction.values()) {
            plugs[side.ordinal()] = new Plug(side);
            controls[side.ordinal()] = Network.newNode(host, Visibility.Network).withComponent("microcontroller").create();
        }
    }

    void connectInternal() {
        Network.joinNewNetwork(host.machine().node());
        host.machine().node().connect(snooper);
    }

    void connectExternal() {
        suspended = true;
        connectInternal();
        for (final Direction side : Direction.values()) {
            if (side == host.facing()) {
                plugs[side.ordinal()].node.remove();
                controls[side.ordinal()].remove();
            } else {
                Network.joinNewNetwork(plugs[side.ordinal()].node);
            }
        }
        suspended = false;
        refreshControls();
    }

    Node sidedNode(final Direction side) {
        return side == null || side == host.facing() ? null : plugs[side.ordinal()].node;
    }

    Component control(final Direction side) {
        return side == null || side == host.facing() ? null : controls[side.ordinal()];
    }

    Connector internalConnector() { return snooper; }

    boolean isSideOpen(final Direction side) { return outputs[side.ordinal()]; }

    boolean setSideOpen(final Direction side, final boolean open) {
        final boolean previous = outputs[side.ordinal()];
        outputs[side.ordinal()] = open;
        host.markChanged();
        return previous;
    }

    void onMessage(final Message message) {
        if (!suspended && MESSAGE.equals(message.name()) && message.source().network() == snooper.network()) {
            for (final Direction side : Direction.values()) {
                if (side != host.facing() && outputs[side.ordinal()]) plugs[side.ordinal()].node.sendToReachable(MESSAGE, message.data());
            }
        }
    }

    void transferEnergy() {
        for (final Direction side : Direction.values()) {
            if (side == host.facing()) continue;
            final Connector connector = (Connector) plugs[side.ordinal()].node;
            final double demand = Math.max(0, snooper.globalBufferSize() - snooper.globalBuffer());
            final double available = demand + connector.changeBuffer(-demand);
            snooper.changeBuffer(available);
        }
    }

    void remove() {
        suspended = true;
        for (final Plug plug : plugs) plug.node.remove();
        for (final Component control : controls) control.remove();
        snooper.remove();
    }

    void load(final CompoundTag tag) {
        remove();
        Arrays.fill(outputs, true);
        final byte[] storedOutputs = tag.getByteArray(OUTPUTS);
        for (int index = 0; index < Math.min(outputs.length, storedOutputs.length); index++) outputs[index] = storedOutputs[index] != 0;
        final ListTag storedPlugs = tag.getList(PLUGS, Tag.TAG_COMPOUND);
        final ListTag storedControls = tag.getList(CONTROLS, Tag.TAG_COMPOUND);
        for (int index = 0; index < plugs.length; index++) {
            if (index < storedPlugs.size()) plugs[index].node.load(storedPlugs.getCompound(index));
            if (index < storedControls.size()) controls[index].load(storedControls.getCompound(index));
        }
        if (tag.contains("oc:storedEnergy", Tag.TAG_ANY_NUMERIC)) {
            // BlockItem merges the portable payload into the freshly created
            // block entity's default NBT, which already contains an empty snooper.
            final CompoundTag portable = tag.getCompound(SNOOPER).copy();
            portable.putDouble("buffer", Math.max(0, tag.getInt("oc:storedEnergy")));
            snooper.load(portable);
        } else if (tag.contains(SNOOPER, Tag.TAG_COMPOUND)) {
            snooper.load(tag.getCompound(SNOOPER));
        } else {
            // Earlier port world saves stored energy on the computer node.
            final CompoundTag legacy = new CompoundTag();
            legacy.putDouble("buffer", tag.getCompound("oc:machine").getCompound("node").getDouble("buffer"));
            snooper.load(legacy);
        }
    }

    void save(final CompoundTag tag) {
        final boolean wasSuspended = suspended;
        suspended = true;
        try {
            final byte[] storedOutputs = new byte[outputs.length];
            final ListTag storedPlugs = new ListTag();
            final ListTag storedControls = new ListTag();
            for (int index = 0; index < plugs.length; index++) {
                storedOutputs[index] = (byte) (outputs[index] ? 1 : 0);
                storedPlugs.add(saveNode(plugs[index].node));
                storedControls.add(saveNode(controls[index]));
            }
            tag.putByteArray(OUTPUTS, storedOutputs);
            tag.put(PLUGS, storedPlugs);
            tag.put(CONTROLS, storedControls);
            tag.put(SNOOPER, saveNode(snooper));
        } finally {
            suspended = wasSuspended;
        }
    }

    private static CompoundTag saveNode(final Node node) {
        final boolean detached = node.network() == null;
        if (node.address() == null) Network.joinNewNetwork(node);
        final CompoundTag tag = new CompoundTag();
        node.save(tag);
        if (detached) node.remove();
        return tag;
    }

    private boolean isPrimary(final Plug plug) {
        if (plug.side == host.facing() || plug.node.network() == null) return false;
        for (final Plug other : plugs) {
            if (other.side != host.facing() && other.node.network() == plug.node.network()) return other == plug;
        }
        return false;
    }

    private void refreshControls() {
        if (suspended || refreshing) return;
        refreshing = true;
        try {
            for (final Plug plug : plugs) {
                final Component control = controls[plug.side.ordinal()];
                if (isPrimary(plug)) {
                    plug.node.connect(control);
                } else {
                    control.remove();
                }
            }
        } finally {
            refreshing = false;
        }
    }

    private final class Plug implements Environment {
        private final Direction side;
        private final Node node;

        private Plug(final Direction side) {
            this.side = side;
            node = Network.newNode(this, Visibility.Network).withConnector().create();
        }

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node connected) { refreshControls(); }
        @Override public void onDisconnect(final Node disconnected) { refreshControls(); }
        @Override public void onMessage(final Message message) {
            if (!suspended && isPrimary(this) && MESSAGE.equals(message.name()) && message.source().network() != snooper.network()) {
                snooper.sendToReachable(MESSAGE, message.data());
            }
        }
    }
}
