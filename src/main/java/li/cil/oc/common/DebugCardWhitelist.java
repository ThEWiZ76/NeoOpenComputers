package li.cil.oc.common;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DebugCardWhitelist {
    private final Path path;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> values = new LinkedHashMap<>();
    private boolean loaded;

    DebugCardWhitelist(final Path path) {
        this.path = path;
    }

    public static DebugCardWhitelist instance() {
        return Holder.INSTANCE;
    }

    public Path path() {
        return path;
    }

    public synchronized Optional<String> nonce(final String player) {
        ensureLoaded();
        return Optional.ofNullable(values.get(normalize(player)));
    }

    public synchronized boolean isWhitelisted(final String player) {
        ensureLoaded();
        return values.containsKey(normalize(player));
    }

    public synchronized Set<String> whitelist() {
        ensureLoaded();
        return Collections.unmodifiableSet(values.keySet());
    }

    public synchronized void add(final String player) throws IOException {
        ensureLoaded();
        values.computeIfAbsent(normalize(player), ignored -> generateNonce());
        save();
    }

    public synchronized void remove(final String player) throws IOException {
        ensureLoaded();
        values.remove(normalize(player));
        save();
    }

    public synchronized void invalidate(final String player) throws IOException {
        ensureLoaded();
        final String normalized = normalize(player);
        if (values.containsKey(normalized)) {
            values.put(normalized, generateNonce());
            save();
        }
    }

    public synchronized void reload() {
        values.clear();
        loaded = false;
        ensureLoaded();
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        values.clear();
        if (Files.isRegularFile(path)) {
            try {
                for (final String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    final String[] parts = line.trim().split(" ", 2);
                    if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                        values.put(normalize(parts[0]), parts[1]);
                    }
                }
            } catch (final IOException e) {
                throw new IllegalStateException("Cannot load debug card whitelist: " + path, e);
            }
        }
        loaded = true;
    }

    private void save() throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            builder.append(entry.getKey()).append(' ').append(entry.getValue()).append(System.lineSeparator());
        }
        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private String generateNonce() {
        final byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (final byte value : bytes) {
            builder.append(Character.forDigit((value >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private static String normalize(final String player) {
        return player == null ? "" : player.toLowerCase(Locale.ROOT);
    }

    private static Path defaultPath() {
        try {
            final Path configPath = FMLPaths.CONFIGDIR.get();
            if (configPath != null) {
                return configPath.resolve("opencomputers").resolve("debug_card_whitelist.txt");
            }
        } catch (final RuntimeException ignored) {
        }
        return Path.of("config").resolve("opencomputers").resolve("debug_card_whitelist.txt");
    }

    private static final class Holder {
        private static final DebugCardWhitelist INSTANCE = new DebugCardWhitelist(defaultPath());
    }
}
