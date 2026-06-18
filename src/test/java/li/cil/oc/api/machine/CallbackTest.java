package li.cil.oc.api.machine;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class CallbackTest {
    @Test
    void callbackAnnotationIsRuntimeMethodAnnotation() {
        assertEquals(RetentionPolicy.RUNTIME, Callback.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, Callback.class.getAnnotation(Target.class).value());
    }

    @Test
    void callbackAnnotationDefaultsMatchUpstreamContract() throws NoSuchMethodException {
        assertEquals("", Callback.class.getMethod("value").getDefaultValue());
        assertFalse((boolean) Callback.class.getMethod("direct").getDefaultValue());
        assertEquals(Integer.MAX_VALUE, Callback.class.getMethod("limit").getDefaultValue());
        assertEquals("", Callback.class.getMethod("doc").getDefaultValue());
        assertFalse((boolean) Callback.class.getMethod("getter").getDefaultValue());
        assertFalse((boolean) Callback.class.getMethod("setter").getDefaultValue());
    }
}
