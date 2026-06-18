package li.cil.oc.api.network;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkMarkerApiTest {
    @Test
    void blacklistedPeripheralExposesBlacklistFlag() {
        BlacklistedPeripheral peripheral = () -> true;

        assertTrue(peripheral.isPeripheralBlacklisted());
    }

    @Test
    void filteredEnvironmentExposesCallbackVisibility() {
        FilteredEnvironment environment = name -> name.startsWith("safe_");

        assertTrue(environment.isCallbackEnabled("safe_read"));
        assertFalse(environment.isCallbackEnabled("delete"));
    }

    @Test
    void simpleComponentExposesComponentName() {
        SimpleComponent component = () -> "disk_drive";

        assertEquals("disk_drive", component.getComponentName());
    }

    @Test
    void skipInjectionIsRuntimeTypeAnnotation() {
        assertEquals(RetentionPolicy.RUNTIME, SimpleComponent.SkipInjection.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, SimpleComponent.SkipInjection.class.getAnnotation(Target.class).value());
    }
}
