package li.cil.oc.client.integration.jei;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CallbackDocumentationTest {
    public static class Parent {
        @Callback(doc = "function(value:number):number -- Parent description")
        public Object[] calculate(Context context, Arguments args) { return null; }
        @Callback public Object[] bare(Context context, Arguments args) { return null; }
        @Callback public String invalid(Context context, Arguments args) { return ""; }
    }
    public static final class Child extends Parent {
        @Callback(value = "calculate", doc = "function(value:number):number; Child description")
        public Object[] renamed(Context context, Arguments args) { return null; }
        @Callback(doc = "Unstructured description")
        public Object[] another(Context context, Arguments args) { return null; }
    }

    @Test void discoversInheritedCallbacksAndChildAliases() {
        final var callbacks = CallbackDocumentation.discover(List.of(Child.class));
        assertEquals(List.of("another", "bare", "calculate"), callbacks.stream().map(CallbackDocumentation.Entry::name).toList());
        final var calculate = callbacks.getLast();
        assertEquals("calculate(value:number):number", calculate.signature());
        assertEquals("Child description", calculate.description());
        assertEquals("Unstructured description", callbacks.getFirst().description());
        assertEquals("", callbacks.get(1).description());
    }

    @Test void ignoresNullEnvironmentsAndDeduplicatesProviders() {
        final var types = new java.util.ArrayList<Class<?>>();
        types.add(null); types.add(Parent.class); types.add(Parent.class);
        assertEquals(2, CallbackDocumentation.discover(types).size());
    }

    @Test void packsDocumentationWithoutDroppingLongPages() {
        final var entries = List.of(new CallbackDocumentation.Entry("first", "first()", "a\nb\nc\nd\ne\nf\ng\nh\ni\nj\nk\nl\nm"),
            new CallbackDocumentation.Entry("second", "second()", "tail"));
        final var pages = CallbackDocumentation.pages(entries, (text, width) -> text.lines().toList());
        assertFalse(pages.isEmpty());
        assertTrue(pages.stream().allMatch(page -> page.size() <= 12));
        final String all = String.join("\n", pages.stream().map(page -> String.join("\n", page)).toList());
        assertTrue(all.contains("first()")); assertTrue(all.contains("  m"));
        assertTrue(all.contains("second()")); assertTrue(all.contains("  tail"));
    }

    @Test void emptyCallbacksHaveNoEmptyRecipePage() {
        assertEquals(List.of(), CallbackDocumentation.pages(List.of(), (text, width) -> List.of(text)));
    }

    @Test void wrappingLeavesRoomForMarginsAndDescriptionIndentation() {
        CallbackDocumentation.pages(List.of(new CallbackDocumentation.Entry("name", "name()", "description")), (text, width) -> {
            final int indent = text.equals("description") ? 8 : 0;
            assertTrue(4 + indent + width <= 156, "Wrapped text must fit inside the160px category with margins");
            return List.of(text);
        });
    }
}
