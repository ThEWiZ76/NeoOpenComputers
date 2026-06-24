package li.cil.oc.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ItemSearch {
    private static final List<StackFocusing> STACK_FOCUSING = new ArrayList<>();
    private static final List<FocusedInput> FOCUSED_INPUTS = new ArrayList<>();

    private ItemSearch() {
    }

    public static AutoCloseable registerStackFocusing(final StackFocusing focusing) {
        STACK_FOCUSING.add(focusing);
        return () -> STACK_FOCUSING.remove(focusing);
    }

    public static AutoCloseable registerFocusedInput(final FocusedInput focusedInput) {
        FOCUSED_INPUTS.add(focusedInput);
        return () -> FOCUSED_INPUTS.remove(focusedInput);
    }

    public static Optional<ItemStack> hoveredStack(final Screen screen, final int mouseX, final int mouseY) {
        for (final StackFocusing focusing : List.copyOf(STACK_FOCUSING)) {
            final Optional<ItemStack> stack = focusing.hoveredStack(screen, mouseX, mouseY);
            if (stack.isPresent()) {
                return stack;
            }
        }
        return Optional.empty();
    }

    public static boolean isInputFocused() {
        for (final FocusedInput focusedInput : List.copyOf(FOCUSED_INPUTS)) {
            if (focusedInput.isFocused()) {
                return true;
            }
        }
        return false;
    }

    static void clearStackFocusing() {
        STACK_FOCUSING.clear();
    }

    static void clearFocusedInput() {
        FOCUSED_INPUTS.clear();
    }

    @FunctionalInterface
    public interface StackFocusing {
        Optional<ItemStack> hoveredStack(Screen screen, int mouseX, int mouseY);
    }

    @FunctionalInterface
    public interface FocusedInput {
        boolean isFocused();
    }
}
