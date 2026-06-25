package li.cil.oc.common;

import li.cil.oc.api.IMC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

public final class ItemChargeImc {
    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.REGISTER_ITEM_CHARGE.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .forEach(ItemChargeImc::register);
    }

    private static void register(final CompoundTag payload) {
        ItemCharges.add(
            staticMethod(payload.getString("canCharge"), ItemStack.class),
            staticMethod(payload.getString("charge"), ItemStack.class, double.class, boolean.class));
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing item charge callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid item charge callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Item charge callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing item charge callback " + methodName, e);
        }
    }

    private ItemChargeImc() {
    }
}
