package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;

public interface Label extends Persistable {
    String getLabel();

    void setLabel(String value);
}
