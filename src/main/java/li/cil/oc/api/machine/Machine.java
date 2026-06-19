package li.cil.oc.api.machine;

import li.cil.oc.api.network.ManagedEnvironment;

import java.util.Map;

public interface Machine extends ManagedEnvironment, Context {
    MachineHost host();

    void onHostChanged();

    Architecture architecture();

    Map<String, String> components();

    int componentCount();

    int maxComponents();

    double getCostPerTick();

    void setCostPerTick(double value);

    String tmpAddress();

    String lastError();

    long worldTime();

    double upTime();

    double cpuTime();

    void beep(short frequency, short duration);

    void beep(String pattern);

    boolean crash(String message);

    Signal popSignal();

    Map<String, Callback> methods(Object value);

    Object[] invoke(String address, String method, Object[] args) throws Exception;

    Object[] invoke(Value value, String method, Object[] args) throws Exception;

    String[] users();

    void addUser(String name) throws Exception;

    boolean removeUser(String name);
}
