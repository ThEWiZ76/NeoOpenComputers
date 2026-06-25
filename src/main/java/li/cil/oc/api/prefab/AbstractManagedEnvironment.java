package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;

public abstract class AbstractManagedEnvironment implements ManagedEnvironment {
    public static final String NODE_TAG = "node";

    private Node node;

    @Override
    public Node node() {
        return node;
    }

    protected void setNode(final Node value) {
        node = value;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (node() != null) {
            node().load(nbt.getCompound(NODE_TAG));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (node() == null) {
            return;
        }

        if (node().address() == null) {
            Network.joinNewNetwork(node());
            saveNode(nbt);
            node().remove();
        } else {
            saveNode(nbt);
        }
    }

    private void saveNode(final CompoundTag nbt) {
        CompoundTag nodeTag = new CompoundTag();
        node().save(nodeTag);
        nbt.put(NODE_TAG, nodeTag);
    }
}
