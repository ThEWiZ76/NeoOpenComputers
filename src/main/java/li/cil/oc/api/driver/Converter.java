package li.cil.oc.api.driver;

import java.util.Map;

/**
 * Converts Java values into map-shaped values a machine architecture can expose.
 */
@FunctionalInterface
public interface Converter {
    void convert(Object value, Map<Object, Object> output);
}
