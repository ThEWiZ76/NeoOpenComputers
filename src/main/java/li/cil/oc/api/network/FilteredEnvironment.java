package li.cil.oc.api.network;

/**
 * Dynamically controls callback exposure for an environment.
 */
@FunctionalInterface
public interface FilteredEnvironment {
    boolean isCallbackEnabled(String name);
}
