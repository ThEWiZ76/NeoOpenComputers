package li.cil.oc.api.manual;

/**
 * Provides manual page content for a path.
 */
@FunctionalInterface
public interface ContentProvider {
    Iterable<String> getContent(String path);
}
