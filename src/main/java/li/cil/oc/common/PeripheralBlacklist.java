package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PeripheralBlacklist {
    private static final Set<String> NAMES = new LinkedHashSet<>();

    public static synchronized void add(final String className) {
        if (className != null && !className.isBlank()) {
            NAMES.add(className);
        }
    }

    public static synchronized boolean isBlacklisted(final String className) {
        return NAMES.contains(className);
    }

    public static synchronized boolean isBlacklisted(final Class<?> peripheralType) {
        if (peripheralType == null) {
            return false;
        }
        for (final String className : NAMES) {
            final Class<?> blockedType = resolve(className);
            if (blockedType != null && blockedType.isAssignableFrom(peripheralType)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> resolve(final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            NeoOpenComputers.LOGGER.warn("Ignoring missing peripheral blacklist class '{}'.", className, e);
            return null;
        }
    }

    private PeripheralBlacklist() {
    }
}
