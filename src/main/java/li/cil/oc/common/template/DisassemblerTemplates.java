package li.cil.oc.common.template;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class DisassemblerTemplates {
    private static final List<DisassemblerTemplate> DEFAULT_TEMPLATES = List.of(new TabletDisassemblerTemplate());
    private static final CopyOnWriteArrayList<DisassemblerTemplate> CUSTOM_TEMPLATES = new CopyOnWriteArrayList<>();

    public static Optional<DisassemblerTemplate> select(final ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (DisassemblerTemplate template : templates()) {
            if (template.matches(stack)) {
                return Optional.of(template);
            }
        }
        return Optional.empty();
    }

    public static ItemStack[] disassemble(final ItemStack stack) {
        return select(stack).map(template -> template.disassemble(stack)).orElseGet(() -> new ItemStack[0]);
    }

    public static List<DisassemblerTemplate> templates() {
        final ArrayList<DisassemblerTemplate> templates = new ArrayList<>(CUSTOM_TEMPLATES);
        templates.addAll(DEFAULT_TEMPLATES);
        return Collections.unmodifiableList(templates);
    }

    public static Set<String> defaultTemplateNames() {
        return DEFAULT_TEMPLATES.stream()
            .map(DisassemblerTemplate::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    public static Registration register(final DisassemblerTemplate template) {
        CUSTOM_TEMPLATES.add(template);
        return () -> CUSTOM_TEMPLATES.remove(template);
    }

    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    private DisassemblerTemplates() {
    }
}
