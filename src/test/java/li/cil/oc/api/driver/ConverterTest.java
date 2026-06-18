package li.cil.oc.api.driver;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConverterTest {
    @Test
    void converterWritesConvertedValuesIntoProvidedMap() {
        Converter converter = (value, output) -> output.put("value", value);
        var output = new HashMap<>();

        converter.convert("screen", output);

        assertEquals("screen", output.get("value"));
    }
}
