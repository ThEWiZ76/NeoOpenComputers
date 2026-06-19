package li.cil.oc.api.network;

public interface ComponentHost extends EnvironmentHost {
    Iterable<Environment> getComponents();
}
