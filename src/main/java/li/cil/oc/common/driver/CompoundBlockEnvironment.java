package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CompoundBlockEnvironment implements ManagedEnvironment {
    private static final String TYPE_HASH_TAG = "typeHash";

    private final String name;
    private final List<Entry> environments;
    private final Node node;
    private final List<ManagedEnvironment> updatingEnvironments;

    public CompoundBlockEnvironment(final String name, final List<Entry> environments) {
        this.name = name;
        this.environments = List.copyOf(environments);
        node = Network.newNode(this, reachability(this.environments))
            .withComponent(name)
            .create();
        updatingEnvironments = this.environments.stream()
            .map(Entry::environment)
            .filter(ManagedEnvironment::canUpdate)
            .toList();
        for (Entry entry : this.environments) {
            if (entry.environment().node() instanceof Component component) {
                component.setVisibility(component.reachability().ordinal() >= Visibility.Neighbors.ordinal()
                    ? Visibility.Neighbors
                    : Visibility.None);
            }
        }
    }

    public String name() {
        return name;
    }

    public List<Entry> environments() {
        return environments;
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public boolean canUpdate() {
        return !updatingEnvironments.isEmpty();
    }

    @Override
    public void update() {
        for (ManagedEnvironment environment : updatingEnvironments) {
            environment.update();
        }
    }

    @Override
    public void onConnect(final Node connectedNode) {
        if (connectedNode == node) {
            for (Entry entry : environments) {
                if (entry.environment().node() != null) {
                    node.connect(entry.environment().node());
                }
            }
        }
    }

    @Override
    public void onDisconnect(final Node disconnectedNode) {
        if (disconnectedNode == node) {
            for (Entry entry : environments) {
                if (entry.environment().node() != null) {
                    entry.environment().node().remove();
                }
            }
        }
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TYPE_HASH_TAG) && nbt.getLong(TYPE_HASH_TAG) != typeHash()) {
            return;
        }
        node.load(nbt);
        for (Entry entry : environments) {
            if (nbt.contains(entry.driver())) {
                entry.environment().load(nbt.getCompound(entry.driver()));
            }
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        nbt.putLong(TYPE_HASH_TAG, typeHash());
        node.save(nbt);
        for (Entry entry : environments) {
            final CompoundTag environmentTag = new CompoundTag();
            entry.environment().save(environmentTag);
            nbt.put(entry.driver(), environmentTag);
        }
    }

    private long typeHash() {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            environments.stream()
                .map(entry -> entry.environment().getClass().getName())
                .sorted()
                .forEach(name -> digest.update(name.getBytes(StandardCharsets.UTF_8)));
            final byte[] hash = digest.digest();
            long result = 0;
            for (int index = 0; index < Long.BYTES; index++) {
                result = (result << 8) | (hash[index] & 0xFFL);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Visibility reachability(final List<Entry> environments) {
        return environments.stream()
            .map(Entry::environment)
            .map(ManagedEnvironment::node)
            .filter(node -> node != null)
            .map(Node::reachability)
            .max(Comparator.comparingInt(Enum::ordinal))
            .orElse(Visibility.None);
    }

    public record Entry(String driver, ManagedEnvironment environment) {
    }
}
