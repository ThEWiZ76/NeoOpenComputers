package li.cil.oc.common.component;

import li.cil.oc.api.network.Packet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LinkedNetwork {
    public static final String DEFAULT_CHANNEL = "creative";

    private static final Map<String, List<Endpoint>> CHANNELS = new LinkedHashMap<>();

    public static void add(final Endpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        remove(endpoint);
        CHANNELS.computeIfAbsent(normalizeChannel(endpoint.linkedChannel()), ignored -> new ArrayList<>()).add(endpoint);
    }

    public static void remove(final Endpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        final var iterator = CHANNELS.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, List<Endpoint>> entry = iterator.next();
            entry.getValue().remove(endpoint);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void send(final String channel, final Endpoint source, final Packet packet) {
        if (packet == null) {
            return;
        }
        for (final Endpoint endpoint : List.copyOf(CHANNELS.getOrDefault(normalizeChannel(channel), List.of()))) {
            if (endpoint != source) {
                endpoint.receiveLinkedPacket(packet);
            }
        }
    }

    public static String normalizeChannel(final String value) {
        return value == null || value.isBlank() ? DEFAULT_CHANNEL : value;
    }

    public interface Endpoint {
        String linkedChannel();

        void receiveLinkedPacket(Packet packet);
    }

    private LinkedNetwork() {
    }
}
