package li.cil.oc.common.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerminalServerRegistry {
    private static final Map<String, TerminalServerRackMountableEnvironment> SERVERS = new HashMap<>();
    private static final List<TerminalServerRackMountableEnvironment> PENDING = new ArrayList<>();

    public static void add(final TerminalServerRackMountableEnvironment server) {
        completePending();
        final String address = addressOf(server);
        if (address == null) {
            if (server != null && !PENDING.contains(server)) {
                PENDING.add(server);
            }
        } else if (!SERVERS.containsKey(address)) {
            SERVERS.put(address, server);
        }
    }

    public static void remove(final TerminalServerRackMountableEnvironment server) {
        completePending();
        PENDING.remove(server);
        SERVERS.values().removeIf(candidate -> candidate == server);
    }

    public static TerminalServerRackMountableEnvironment find(final String address) {
        completePending();
        if (address == null || address.isBlank()) {
            return null;
        }
        final TerminalServerRackMountableEnvironment server = SERVERS.get(address);
        if (server == null || !address.equals(addressOf(server))) {
            SERVERS.remove(address);
            return null;
        }
        return server;
    }

    public static void clear() {
        SERVERS.clear();
        PENDING.clear();
    }

    private static String addressOf(final TerminalServerRackMountableEnvironment server) {
        return server == null || server.node() == null ? null : server.node().address();
    }

    private static void completePending() {
        final List<TerminalServerRackMountableEnvironment> promoted = new ArrayList<>();
        for (final TerminalServerRackMountableEnvironment server : PENDING) {
            final String address = addressOf(server);
            if (address != null) {
                promoted.add(server);
                SERVERS.putIfAbsent(address, server);
            }
        }
        PENDING.removeAll(promoted);
    }

    private TerminalServerRegistry() {
    }
}
