package li.cil.oc.api.machine;

import li.cil.oc.api.Persistable;

public interface Value extends Persistable {
    Object apply(Context context, Arguments arguments);

    void unapply(Context context, Arguments arguments);

    Object[] call(Context context, Arguments arguments);

    void dispose(Context context);
}
