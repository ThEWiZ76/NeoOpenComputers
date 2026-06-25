package li.cil.oc.common;

import li.cil.oc.api.IMC;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

public final class InkProviderImc {
    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.REGISTER_INK_PROVIDER.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(methodName -> staticMethod(methodName, ItemStack.class))
            .forEach(InkProviders::add);
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing ink provider callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid ink provider callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Ink provider callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing ink provider callback " + methodName, e);
        }
    }

    private InkProviderImc() {
    }
}
