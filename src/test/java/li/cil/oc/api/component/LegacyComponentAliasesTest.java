package li.cil.oc.api.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyComponentAliasesTest {
    @Test
    void deprecatedComponentKeyboardAliasExtendsInternalKeyboardContract() throws ClassNotFoundException {
        Class<?> keyboard = Class.forName("li.cil.oc.api.component.Keyboard");
        Class<?> usabilityChecker = Class.forName("li.cil.oc.api.component.Keyboard$UsabilityChecker");

        assertTrue(li.cil.oc.api.internal.Keyboard.class.isAssignableFrom(keyboard));
        assertTrue(li.cil.oc.api.internal.Keyboard.UsabilityChecker.class.isAssignableFrom(usabilityChecker));
    }

    @Test
    void deprecatedComponentTextBufferAliasExtendsInternalTextBufferContract() throws ClassNotFoundException {
        Class<?> textBuffer = Class.forName("li.cil.oc.api.component.TextBuffer");

        assertTrue(li.cil.oc.api.internal.TextBuffer.class.isAssignableFrom(textBuffer));
    }
}
