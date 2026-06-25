package li.cil.oc.common.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProgramLocations {
    private static final String[] DEFAULT_ARCHITECTURES = {"Lua 5.2", "Lua 5.3", "LuaJ"};
    private static final Map<String, Map<String, String>> ARCHITECTURE_LOCATIONS = new LinkedHashMap<>();
    private static final Map<String, String> GLOBAL_LOCATIONS = new LinkedHashMap<>();

    private ProgramLocations() {
    }

    public static synchronized void addMapping(final String program, final String label, final String... architectures) {
        if (program == null || label == null) {
            return;
        }
        if (architectures == null || architectures.length == 0) {
            GLOBAL_LOCATIONS.put(program, label);
            return;
        }
        for (String architecture : architectures) {
            if (architecture != null) {
                ARCHITECTURE_LOCATIONS
                    .computeIfAbsent(architecture, key -> new LinkedHashMap<>())
                    .put(program, label);
            }
        }
    }

    public static synchronized void registerDefaults() {
        addMapping("build", "builder", DEFAULT_ARCHITECTURES);
        addMapping("dig", "dig", DEFAULT_ARCHITECTURES);
        addMapping("base64", "data", DEFAULT_ARCHITECTURES);
        addMapping("deflate", "data", DEFAULT_ARCHITECTURES);
        addMapping("gpg", "data", DEFAULT_ARCHITECTURES);
        addMapping("inflate", "data", DEFAULT_ARCHITECTURES);
        addMapping("md5sum", "data", DEFAULT_ARCHITECTURES);
        addMapping("sha256sum", "data", DEFAULT_ARCHITECTURES);
        addMapping("refuel", "generator", DEFAULT_ARCHITECTURES);
        addMapping("irc", "irc", DEFAULT_ARCHITECTURES);
        addMapping("maze", "maze", DEFAULT_ARCHITECTURES);
        addMapping("arp", "network", DEFAULT_ARCHITECTURES);
        addMapping("ifconfig", "network", DEFAULT_ARCHITECTURES);
        addMapping("ping", "network", DEFAULT_ARCHITECTURES);
        addMapping("route", "network", DEFAULT_ARCHITECTURES);
        addMapping("opl-flash", "openloader", DEFAULT_ARCHITECTURES);
        addMapping("oppm", "oppm", DEFAULT_ARCHITECTURES);
    }

    static synchronized List<Mapping> mappings(final String architecture) {
        final Map<String, String> merged = new LinkedHashMap<>();
        if (architecture != null) {
            merged.putAll(ARCHITECTURE_LOCATIONS.getOrDefault(architecture, Map.of()));
        }
        merged.putAll(GLOBAL_LOCATIONS);
        final List<Mapping> mappings = new ArrayList<>(merged.size());
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            mappings.add(new Mapping(entry.getKey(), entry.getValue()));
        }
        return mappings;
    }

    public static synchronized Map<String, String> mappingsByProgram(final String architecture) {
        final Map<String, String> merged = new LinkedHashMap<>();
        if (architecture != null) {
            merged.putAll(ARCHITECTURE_LOCATIONS.getOrDefault(architecture, Map.of()));
        }
        merged.putAll(GLOBAL_LOCATIONS);
        return Map.copyOf(merged);
    }

    public static synchronized void clear() {
        ARCHITECTURE_LOCATIONS.clear();
        GLOBAL_LOCATIONS.clear();
    }

    record Mapping(String program, String label) {
    }
}
