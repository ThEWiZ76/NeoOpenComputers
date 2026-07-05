package li.cil.oc.common.template;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class AssemblerTemplates {
    private static final List<AssemblerTemplate> DEFAULT_TEMPLATES = List.of(new TabletAssemblerTemplate(), new MicrocontrollerAssemblerTemplate(), new RobotAssemblerTemplate(), new DroneAssemblerTemplate());
    private static final CopyOnWriteArrayList<AssemblerTemplate> CUSTOM_TEMPLATES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Predicate<ItemStack>> TEMPLATE_FILTERS = new CopyOnWriteArrayList<>();

    public static Optional<AssemblerTemplate> select(final ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (Predicate<ItemStack> filter : TEMPLATE_FILTERS) {
            if (!filter.test(stack)) {
                return Optional.empty();
            }
        }
        for (AssemblerTemplate template : templates()) {
            if (template.matches(stack)) {
                return Optional.of(template);
            }
        }
        return Optional.empty();
    }

    public static List<AssemblerTemplate> templates() {
        final ArrayList<AssemblerTemplate> templates = new ArrayList<>(CUSTOM_TEMPLATES);
        templates.addAll(DEFAULT_TEMPLATES);
        return Collections.unmodifiableList(templates);
    }

    public static Set<String> defaultTemplateNames() {
        return DEFAULT_TEMPLATES.stream()
            .map(AssemblerTemplate::name)
            .collect(Collectors.toUnmodifiableSet());
    }

    public static Registration register(final AssemblerTemplate template) {
        CUSTOM_TEMPLATES.add(template);
        return () -> CUSTOM_TEMPLATES.remove(template);
    }

    public static Registration registerFilter(final Predicate<ItemStack> filter) {
        TEMPLATE_FILTERS.add(filter);
        return () -> TEMPLATE_FILTERS.remove(filter);
    }

    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    private AssemblerTemplates() {
    }
}
