package li.cil.oc.common.gametest;

import li.cil.oc.common.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class JeiSubtypeGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return List.of();
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(
                new TestFunction("jeiSubtypes", "jei_assembled_component_subtypes", "neoopencomputers:empty", 100, 0, true, Loaded::assembled),
                new TestFunction("jeiSubtypes", "jei_floppy_factory_subtypes", "neoopencomputers:empty", 100, 0, true, Loaded::floppies),
                new TestFunction("jeiSubtypes", "jei_subtype_registration_scope", "neoopencomputers:empty", 100, 0, true, Loaded::scope));
        }

        private static Map<Item, mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>> registered(GameTestHelper helper) {
            final Map<Item, mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>> result = new java.util.LinkedHashMap<>();
            final var registration = (mezz.jei.api.registration.ISubtypeRegistration) java.lang.reflect.Proxy.newProxyInstance(
                JeiSubtypeGameTests.class.getClassLoader(), new Class<?>[]{mezz.jei.api.registration.ISubtypeRegistration.class}, (proxy, method, args) -> {
                    helper.assertTrue(method.getName().equals("registerSubtypeInterpreter"), "Unexpected subtype registration call");
                    final int base = args.length == 3 ? 1 : 0;
                    @SuppressWarnings("unchecked") final var interpreter = (mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>) args[base + 1];
                    helper.assertTrue(result.put((Item) args[base], interpreter) == null, "JEI subtype registered twice");
                    return null;
                });
            new li.cil.oc.client.integration.jei.OpenComputersJeiPlugin().registerItemSubtypes(registration);
            return result;
        }

        private static List<Item> assembledItems() {
            return List.of(ModItems.ROBOT.get(), ModItems.MICROCONTROLLER_TIER1.get(), ModItems.MICROCONTROLLER_TIER2.get(), ModItems.MICROCONTROLLER_CREATIVE.get(), ModItems.DRONE.get(), ModItems.TABLET.get());
        }

        static void assembled(GameTestHelper helper) {
            final var registered = registered(helper);
            for (var item : assembledItems()) {
                final var interpreter = registered.get(item);
                helper.assertTrue(interpreter != null, "Assembled item lacks JEI subtype interpreter: " + item);
                for (var context : mezz.jei.api.ingredients.subtypes.UidContext.values()) {
                    final var stack = new ItemStack(item);
                    final var data = new CompoundTag(); data.putString("cpu", "fixture-cpu-1"); data.putInt("energy", 20);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                    final var key = interpreter.getSubtypeData(stack, context);
                    final var copy = stack.copy(); copy.setCount(3);
                    helper.assertTrue(key != null && key.equals(interpreter.getSubtypeData(copy, context)), "Assembled subtype included stack count or lost component equality");
                    data.putString("cpu", "fixture-cpu-2"); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                    helper.assertTrue(!key.equals(interpreter.getSubtypeData(stack, context)) && key.equals(interpreter.getSubtypeData(copy, context)), "Assembled subtype lost hardware distinction or changed an existing key");
                    copy.set(DataComponents.CUSTOM_NAME, Component.literal("fixture-name"));
                    helper.assertTrue(!key.equals(interpreter.getSubtypeData(copy, context)), "Full assembled subtype ignored another item component");
                }
            }
            helper.succeed();
        }

        static void floppies(GameTestHelper helper) {
            final var interpreter = registered(helper).get(ModItems.FLOPPY.get());
            helper.assertTrue(interpreter != null, "Floppy lacks JEI subtype interpreter");
            for (var context : mezz.jei.api.ingredients.subtypes.UidContext.values()) {
                final var disk = new ItemStack(ModItems.FLOPPY.get());
                final var tag = new CompoundTag(); tag.putString("oc:label", "first"); tag.putString("node", "fixture-node-1");
                disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                helper.assertTrue(interpreter.getSubtypeData(disk, context) == null, "Ordinary floppy split by mutable label/storage data");
                tag.putString("oc:factory", "neoopencomputers:loot/openos"); disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                final var openos = interpreter.getSubtypeData(disk, context);
                tag.putString("oc:label", "second"); tag.putString("node", "fixture-node-2"); disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                disk.set(DataComponents.CUSTOM_NAME, Component.literal("renamed"));
                helper.assertTrue(openos != null && openos.equals(interpreter.getSubtypeData(disk, context)), "Loot disk split by mutable name or storage data");
                tag.putString("oc:factory", "neoopencomputers:loot/oppm"); disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                final var oppm = interpreter.getSubtypeData(disk, context);
                helper.assertTrue(!openos.equals(oppm), "Different floppy factories collapsed");
                tag.putString("oc:lootFactory", "neoopencomputers:loot/openos"); disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                helper.assertTrue(openos.equals(interpreter.getSubtypeData(disk, context)), "Legacy factory did not follow disk-driver precedence");
                tag.putString("oc:lootPath", "oppm"); disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                helper.assertTrue(oppm.equals(interpreter.getSubtypeData(disk, context)), "Legacy bundled path did not override other factory fields like the disk driver");
            }
            helper.succeed();
        }

        static void scope(GameTestHelper helper) {
            final var registered = registered(helper);
            helper.assertTrue(registered.size() == 7 && registered.keySet().containsAll(assembledItems()) && registered.containsKey(ModItems.FLOPPY.get()), "Wrong JEI subtype registration scope");
            helper.assertTrue(!registered.containsKey(ModItems.HDD_TIER1.get()) && !registered.containsKey(ModItems.EEPROM.get()), "Unrelated mutable media split into JEI subtypes");
            helper.succeed();
        }
    }
}
