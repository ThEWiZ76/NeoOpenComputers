package li.cil.oc.common.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProgramLocations {
    private static final Map<String, Map<String, String>> ARCHITECTURE_LOCATIONS = new LinkedHashMap<>();
    private static final Map<String, String> GLOBAL_LOCATIONS = new LinkedHashMap<>();

    private ProgramLocations() {
    }

    static synchronized void addMapping(final String program, final String label, final String... architectures) {
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

    static synchronized void clear() {
        ARCHITECTURE_LOCATIONS.clear();
        GLOBAL_LOCATIONS.clear();
    }

    record Mapping(String program, String label) {
    }
}
