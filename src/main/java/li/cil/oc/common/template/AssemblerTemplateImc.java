package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.IMC;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class AssemblerTemplateImc {
    private static final int ANY_TIER = Integer.MAX_VALUE;

    public static List<AssemblerTemplates.Registration> process(final Stream<InterModComms.IMCMessage> messages) {
        final List<AssemblerTemplates.Registration> registrations = new ArrayList<>();
        messages
            .filter(message -> IMC.REGISTER_ASSEMBLER_TEMPLATE.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .map(AssemblerTemplateImc::templateFrom)
            .forEach(template -> registrations.add(AssemblerTemplates.register(template)));
        return registrations;
    }

    private static AssemblerTemplate templateFrom(final CompoundTag tag) {
        final String name = tag.contains("name") ? tag.getString("name") : "imc";
        final Method select = staticMethod(tag.getString("select"), ItemStack.class);
        final Method validate = staticMethod(tag.getString("validate"), Container.class);
        final Method assemble = staticMethod(tag.getString("assemble"), Container.class);
        final Class<? extends EnvironmentHost> hostClass = hostClass(tag.getString("hostClass"));
        final SlotRule[] containerSlots = rules(tag.getList("containerSlots", net.minecraft.nbt.Tag.TAG_COMPOUND), Slot.Container, hostClass, AssemblerBlockEntity.CONTAINER_SLOT_COUNT);
        final SlotRule[] upgradeSlots = rules(tag.getList("upgradeSlots", net.minecraft.nbt.Tag.TAG_COMPOUND), Slot.Upgrade, hostClass, AssemblerBlockEntity.UPGRADE_SLOT_COUNT);
        final SlotRule[] componentSlots = rules(tag.getList("componentSlots", net.minecraft.nbt.Tag.TAG_COMPOUND), null, hostClass, AssemblerBlockEntity.COMPONENT_SLOT_COUNT);
        return new CallbackTemplate(name, select, validate, assemble, containerSlots, upgradeSlots, componentSlots);
    }

    private static SlotRule[] rules(final ListTag tags, final String kindOverride, final Class<? extends EnvironmentHost> hostClass, final int count) {
        final SlotRule[] rules = new SlotRule[count];
        for (int index = 0; index < count && index < tags.size(); index++) {
            final CompoundTag tag = tags.getCompound(index);
            final String kind = kindOverride != null ? kindOverride : tag.getString("type");
            final int tier = tag.contains("tier") ? tag.getInt("tier") : ANY_TIER;
            final Method validator = tag.contains("validate") ? staticMethod(tag.getString("validate"), Container.class, int.class, int.class, ItemStack.class) : null;
            rules[index] = new SlotRule(kind, tier, validator, hostClass);
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends EnvironmentHost> hostClass(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return (Class<? extends EnvironmentHost>) Class.forName(name).asSubclass(EnvironmentHost.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Missing assembler host class " + name, e);
        }
    }

    private static Method staticMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("Missing assembler callback method name");
        }
        final int separator = methodName.lastIndexOf('.');
        if (separator <= 0 || separator >= methodName.length() - 1) {
            throw new IllegalArgumentException("Invalid assembler callback method " + methodName);
        }
        try {
            final Class<?> owner = Class.forName(methodName.substring(0, separator));
            final Method method = owner.getMethod(methodName.substring(separator + 1), parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("Assembler callback must be static: " + methodName);
            }
            return method;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Missing assembler callback " + methodName, e);
        }
    }

    private static boolean invokeBoolean(final Method method, final Object fallback, final Object... args) {
        final Object result = invoke(method, fallback, args);
        if (result instanceof Boolean value) {
            return value;
        }
        if (result instanceof Object[] values && values.length > 0 && values[0] instanceof Boolean value) {
            return value;
        }
        return false;
    }

    private static Object invoke(final Method method, final Object fallback, final Object... args) {
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            return fallback;
        }
    }

    private static final class CallbackTemplate implements AssemblerTemplate {
        private final String name;
        private final Method select;
        private final Method validate;
        private final Method assemble;
        private final SlotRule[] containerSlots;
        private final SlotRule[] upgradeSlots;
        private final SlotRule[] componentSlots;
        private double lastEnergyRequired = 1D;

        private CallbackTemplate(
            final String name,
            final Method select,
            final Method validate,
            final Method assemble,
            final SlotRule[] containerSlots,
            final SlotRule[] upgradeSlots,
            final SlotRule[] componentSlots) {
            this.name = name;
            this.select = select;
            this.validate = validate;
            this.assemble = assemble;
            this.containerSlots = containerSlots;
            this.upgradeSlots = upgradeSlots;
            this.componentSlots = componentSlots;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean matches(final ItemStack stack) {
            return invokeBoolean(select, false, stack);
        }

        @Override
        public boolean validate(final AssemblerBlockEntity assembler) {
            return invokeBoolean(validate, false, assembler);
        }

        @Override
        public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
            if (slot == AssemblerBlockEntity.SLOT_TEMPLATE) {
                return matches(stack);
            }
            final SlotRule rule = ruleFor(slot);
            return rule != null && rule.validate(assembler, slot, stack);
        }

        @Override
        public ItemStack assemble(final AssemblerBlockEntity assembler) {
            final Object result = invoke(assemble, null, assembler);
            lastEnergyRequired = 1D;
            if (result instanceof ItemStack stack) {
                return stack;
            }
            if (result instanceof Object[] values && values.length > 0 && values[0] instanceof ItemStack stack) {
                if (values.length > 1 && values[1] instanceof Number number) {
                    lastEnergyRequired = number.doubleValue();
                }
                return stack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public double energyRequired(final AssemblerBlockEntity assembler) {
            return lastEnergyRequired;
        }

        private SlotRule ruleFor(final int slot) {
            if (slot >= AssemblerBlockEntity.SLOT_CONTAINER_START && slot < AssemblerBlockEntity.SLOT_CONTAINER_START + AssemblerBlockEntity.CONTAINER_SLOT_COUNT) {
                return containerSlots[slot - AssemblerBlockEntity.SLOT_CONTAINER_START];
            }
            if (slot >= AssemblerBlockEntity.SLOT_UPGRADE_START && slot < AssemblerBlockEntity.SLOT_UPGRADE_START + AssemblerBlockEntity.UPGRADE_SLOT_COUNT) {
                return upgradeSlots[slot - AssemblerBlockEntity.SLOT_UPGRADE_START];
            }
            if (slot >= AssemblerBlockEntity.SLOT_COMPONENT_START && slot < AssemblerBlockEntity.SLOT_COMPONENT_START + AssemblerBlockEntity.COMPONENT_SLOT_COUNT) {
                return componentSlots[slot - AssemblerBlockEntity.SLOT_COMPONENT_START];
            }
            return null;
        }
    }

    private record SlotRule(String kind, int tier, Method validator, Class<? extends EnvironmentHost> hostClass) {
        private boolean validate(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
            if (validator != null) {
                return invokeBoolean(validator, false, assembler, slot, tier, stack);
            }
            final DriverItem driver = hostClass == null ? Driver.driverFor(stack) : Driver.driverFor(stack, hostClass);
            return driver != null && kind.equals(driver.slot(stack)) && driver.tier(stack) <= tier;
        }
    }

    private AssemblerTemplateImc() {
    }
}
