package li.cil.oc.api.machine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

final class LimitReachedExceptionTest {
    @Test
    void isCheckedExceptionWithNoDefaultMessage() {
        var exception = new LimitReachedException();

        assertInstanceOf(Exception.class, exception);
        assertNull(exception.getMessage());
    }
}
