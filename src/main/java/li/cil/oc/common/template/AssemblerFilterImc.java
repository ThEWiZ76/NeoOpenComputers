package li.cil.oc.common.template;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.IMC;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

public final class AssemblerFilterImc {
    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.REGISTER_ASSEMBLER_FILTER.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .forEach(AssemblerFilterImc::register);
    }

    private static void register(final String methodName) {
        final Method method = staticMethod(methodName, ItemStack.class);
        AssemblerTemplates.registerFilter(stack -> invokeBoolean(method, stack));
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing assembler filter callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid assembler filter callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Assembler filter callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing assembler filter callback " + methodName, e);
        }
    }

    private static boolean invokeBoolean(final Method method, final ItemStack stack) {
        try {
            final Object result = method.invoke(null, stack);
            return !(result instanceof Boolean value) || value;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            NeoOpenComputers.LOGGER.warn("Error invoking assembler filter {}.", method.getName(), e);
            return true;
        }
    }

    private AssemblerFilterImc() {
    }
}
