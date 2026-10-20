package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** Original shared robot/drone default names, loaded from the bundled upstream list. */
public final class AgentNames {
    private static final List<String> NAMES = load();

    private AgentNames() { }

    public static String resolve(final String name) {
        return name == null || name.isEmpty() ? randomName() : name;
    }

    public static String randomName() {
        return randomName(ThreadLocalRandom.current());
    }

    static String randomName(final RandomGenerator random) { return choose(NAMES, random); }

    static String choose(final List<String> names, final RandomGenerator random) {
        return names.isEmpty() ? "Robot" : names.get(random.nextInt(names.size()));
    }

    static List<String> parse(final Reader input) throws IOException {
        final var names = new ArrayList<String>();
        final var reader = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        String line;
        while ((line = reader.readLine()) != null) {
            final int comment = line.indexOf('#');
            final String name = (comment < 0 ? line : line.substring(0, comment)).trim();
            if (!name.isEmpty()) names.add(name);
        }
        return List.copyOf(names);
    }

    private static List<String> load() {
        try (final var stream = AgentNames.class.getResourceAsStream("/assets/neoopencomputers/robot.names")) {
            if (stream == null) throw new IOException("Missing robot.names");
            return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            NeoOpenComputers.LOGGER.warn("Failed loading robot name list", exception);
            return List.of();
        }
    }
}
