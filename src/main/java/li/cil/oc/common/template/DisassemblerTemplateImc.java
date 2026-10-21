package li.cil.oc.common.template;

import li.cil.oc.api.IMC;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DisassemblerTemplateImc {
    public static List<DisassemblerTemplates.Registration> process(final Stream<InterModComms.IMCMessage> messages) {
        final List<DisassemblerTemplates.Registration> registrations = new ArrayList<>();
        messages
            .filter(message -> IMC.REGISTER_DISASSEMBLER_TEMPLATE.equals(message.method()))
            .forEach(message -> {
                try {
                    final Object payload = message.messageSupplier().get();
                    if (payload instanceof CompoundTag tag) registrations.add(DisassemblerTemplates.register(templateFrom(tag)));
                } catch (RuntimeException failure) {
                    NeoOpenComputers.LOGGER.warn("Failed registering disassembler template", failure);
                }
            });
        return registrations;
    }

    private static DisassemblerTemplate templateFrom(final CompoundTag tag) {
        final String name = tag.contains("name") ? tag.getString("name") : "imc";
        final Method select = staticMethod(tag.getString("select"), ItemStack.class);
        final Method disassemble = disassemblerMethod(tag.getString("disassemble"));
        return new CallbackTemplate(name, select, disassemble);
    }

    private static Method disassemblerMethod(final String name) {
        try { return staticMethod(name, ItemStack.class, ItemStack[].class); }
        catch (IllegalArgumentException failure) {
            if (failure.getCause() instanceof NoSuchMethodException) return staticMethod(name, ItemStack.class);
            throw failure;
        }
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing disassembler callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid disassembler callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Disassembler callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing disassembler callback " + methodName, e);
        }
    }

    private static boolean invokeBoolean(final Method method, final Object... args) {
        final Object result = invoke(method, args);
        if (result instanceof Boolean value) {
            return value;
        }
        if (result instanceof Object[] values && values.length > 0 && values[0] instanceof Boolean value) {
            return value;
        }
        return false;
    }

    private static DisassemblyResult outputs(final Object result) {
        if (result instanceof ItemStack[] stacks) return new DisassemblyResult(stacks, null);
        if (result instanceof Object[] pair && pair.length == 2
            && (pair[0] instanceof ItemStack[] || pair[1] instanceof ItemStack[])
            && (pair[0] instanceof ItemStack || pair[0] instanceof ItemStack[])
            && (pair[1] instanceof ItemStack || pair[1] instanceof ItemStack[])) {
            return new DisassemblyResult(stacks(pair[0]), stacks(pair[1]));
        }
        return new DisassemblyResult(stacks(result), null);
    }

    private static ItemStack[] stacks(final Object result) {
        if (result instanceof ItemStack itemStack) {
            return new ItemStack[]{itemStack};
        }
        if (result instanceof ItemStack[] stacks) {
            return stacks;
        }
        if (result instanceof Iterable<?> iterable) {
            final ArrayList<ItemStack> stacks = new ArrayList<>();
            for (Object value : iterable) {
                if (value instanceof ItemStack itemStack) {
                    stacks.add(itemStack);
                }
            }
            return stacks.toArray(ItemStack[]::new);
        }
        if (result instanceof Object[] values) {
            final ArrayList<ItemStack> stacks = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof ItemStack itemStack) {
                    stacks.add(itemStack);
                }
            }
            return stacks.toArray(ItemStack[]::new);
        }
        return new ItemStack[0];
    }

    private static Object invoke(final Method method, final Object... args) {
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error fatal) throw fatal;
            return null;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return null;
        }
    }

    private record CallbackTemplate(String name, Method select, Method disassemble) implements DisassemblerTemplate {
        @Override
        public boolean matches(final ItemStack stack) {
            return invokeBoolean(select, stack);
        }

        @Override
        public ItemStack[] disassemble(final ItemStack stack) {
            return plan(stack, new ItemStack[0]).queued();
        }

        @Override
        public DisassemblyResult plan(final ItemStack stack, final ItemStack[] ingredients) {
            final Object result = disassemble.getParameterCount() == 2 ? invoke(disassemble, stack, ingredients) : invoke(disassemble, stack);
            return outputs(result);
        }
    }

    private DisassemblerTemplateImc() {
    }
}
