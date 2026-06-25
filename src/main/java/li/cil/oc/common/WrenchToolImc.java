package li.cil.oc.common;

import li.cil.oc.api.IMC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

public final class WrenchToolImc {
    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages.forEach(message -> {
            if (IMC.REGISTER_WRENCH_TOOL_CHECK.equals(message.method())) {
                registerCheck(message.messageSupplier().get());
            } else if (IMC.REGISTER_WRENCH_TOOL.equals(message.method())) {
                registerUsage(message.messageSupplier().get());
            }
        });
    }

    private static void registerCheck(final Object payload) {
        if (payload instanceof String methodName) {
            WrenchTools.addCheck(staticMethod(methodName, ItemStack.class));
        }
    }

    private static void registerUsage(final Object payload) {
        if (payload instanceof String methodName) {
            WrenchTools.addUsage(staticMethod(methodName, Player.class, BlockPos.class, boolean.class));
        }
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing wrench callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid wrench callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Wrench callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing wrench callback " + methodName, e);
        }
    }

    private WrenchToolImc() {
    }
}
