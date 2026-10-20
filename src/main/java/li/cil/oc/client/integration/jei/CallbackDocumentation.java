package li.cil.oc.client.integration.jei;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/** Class-only documentation discovery: never instantiate item environments on the client. */
public final class CallbackDocumentation {
    private static final Pattern SIGNATURE = Pattern.compile("(?s)^function(\\(.*?\\).*?)(?: -- |; )(.*)$");

    public record Entry(String name, String signature, String description) {}

    public static List<Entry> discover(Collection<Class<?>> environments) {
        final var entries = new TreeMap<String, Entry>();
        for (Class<?> environment : environments) {
            for (Class<?> type = environment; type != null; type = type.getSuperclass()) {
                for (final var method : type.getDeclaredMethods()) {
                    final Callback callback = method.getAnnotation(Callback.class);
                    if (callback == null || !Modifier.isPublic(method.getModifiers()) || method.getReturnType() != Object[].class ||
                        !java.util.Arrays.equals(method.getParameterTypes(), new Class<?>[]{Context.class, Arguments.class})) continue;
                    final String name = callback.value().trim().isEmpty() ? method.getName() : callback.value();
                    final var matcher = SIGNATURE.matcher(callback.doc());
                    entries.putIfAbsent(name, matcher.matches() ? new Entry(name, name + matcher.group(1), matcher.group(2)) :
                        new Entry(name, name, callback.doc()));
                }
            }
        }
        return List.copyOf(entries.values());
    }

    public static List<List<String>> pages(List<Entry> entries, BiFunction<String, Integer, List<String>> wrap) {
        final List<List<String>> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (final Entry entry : entries) {
            final List<String> lines = new ArrayList<>(wrap.apply(entry.signature(), 152));
            if (!entry.description().isBlank()) {
                for (final String line : wrap.apply(entry.description(), 144)) lines.add("  " + line);
            }
            if (!page.isEmpty()) {
                if (page.size() + 1 + lines.size() > 12) {
                    pages.add(List.copyOf(page)); page = new ArrayList<>();
                } else page.add("");
            }
            for (final String line : lines) {
                if (page.size() == 12) { pages.add(List.copyOf(page)); page = new ArrayList<>(); }
                page.add(line);
            }
        }
        if (!page.isEmpty()) pages.add(List.copyOf(page));
        return List.copyOf(pages);
    }

    private CallbackDocumentation() {}
}
