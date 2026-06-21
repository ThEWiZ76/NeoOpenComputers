package li.cil.oc.common.component;

import java.util.HashMap;
import java.util.Map;

public final class TerminalServerRegistry {
    private static final Map<String, TerminalServerRackMountableEnvironment> SERVERS = new HashMap<>();

    public static void add(final TerminalServerRackMountableEnvironment server) {
        final String address = addressOf(server);
        if (address != null && !SERVERS.containsKey(address)) {
            SERVERS.put(address, server);
        }
    }

    public static void remove(final TerminalServerRackMountableEnvironment server) {
        SERVERS.values().removeIf(candidate -> candidate == server);
    }

    public static TerminalServerRackMountableEnvironment find(final String address) {
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
    }

    private static String addressOf(final TerminalServerRackMountableEnvironment server) {
        return server == null || server.node() == null ? null : server.node().address();
    }

    private TerminalServerRegistry() {
    }
}
