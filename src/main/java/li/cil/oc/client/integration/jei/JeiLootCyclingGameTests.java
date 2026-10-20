package li.cil.oc.client.integration.jei;

import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class JeiLootCyclingGameTests {
    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return List.of();
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(
                new TestFunction("jeiLootCycling", "jei_loot_crafting_extension_registered", "neoopencomputers:empty", 100, 0, true, Loaded::registration),
                new TestFunction("jeiLootCycling", "jei_loot_crafting_pairs_match_actual_recipe", "neoopencomputers:empty", 100, 0, true, Loaded::pairs),
                new TestFunction("jeiLootCycling", "jei_loot_without_outputs_has_no_layout", "neoopencomputers:empty", 100, 0, true, Loaded::empty));
        }

        static void pairs(GameTestHelper helper) {
            isolated(helper, true, registry -> {
                registry.registerFloppy("jei-cycling-addon", net.minecraft.world.item.DyeColor.BLUE, () -> null, true);
                final var excluded = registry.registerFloppy("jei-cycling-excluded", net.minecraft.world.item.DyeColor.RED, () -> null, false);
                final var candidates = new java.util.ArrayList<>(registry.disksForCycling());
                candidates.add(excluded); candidates.add(new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.FLOPPY.get()));
                final var extension = new LootDiskCyclingExtension(() -> candidates, helper::getLevel);
                final var holder = holder(); final var layout = new Layout();
                helper.assertTrue(extension.isHandled(holder), "Real registered loot recipe was not handled");
                extension.setRecipe(holder, layout.builder, grid(), null);
                final var disks = layout.slots.get(0); final var wrench = layout.slots.get(1);
                final var output = layout.slots.get(9);
                helper.assertTrue(disks.items.size() == candidates.size() - 1 && output.items.size() == disks.items.size(), "Loot layout omitted a registered/opt-out input or included a plain floppy");
                helper.assertTrue(wrench.items.size() == 1 && wrench.items.getFirst().is(li.cil.oc.common.ModItems.WRENCH.get()), "JEI loot recipe lacks its wrench input");
                helper.assertTrue(layout.link != null && layout.link.length == 2 && layout.link[0] == disks.builder && layout.link[1] == output.builder, "JEI did not link each disk input to its corresponding output");
                for (int index = 0; index < disks.items.size(); index++) {
                    final var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(disks.items.get(index), wrench.items.getFirst()));
                    final var expected = holder.value().assemble(input, helper.getLevel().registryAccess());
                    helper.assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(output.items.get(index), expected), "Displayed loot output differs from actual crafting result");
                    helper.assertTrue(!net.minecraft.world.item.ItemStack.isSameItemSameComponents(output.items.get(index), excluded), "Opted-out disk became a JEI cycling output");
                    helper.assertTrue(holder.value().getRemainingItems(input).get(1).is(li.cil.oc.common.ModItems.WRENCH.get()), "Displayed recipe consumes its wrench");
                }
                candidates.getFirst().setCount(0);
                helper.assertTrue(!disks.items.getFirst().isEmpty() && !registry.disksForCycling().getFirst().isEmpty(), "JEI retained/mutated a foreign disk stack");
            });
        }

        static void empty(GameTestHelper helper) {
            isolated(helper, false, registry -> {
                final var excluded = registry.registerFloppy("jei-excluded-only", net.minecraft.world.item.DyeColor.BLUE, () -> null, false);
                final var extension = new LootDiskCyclingExtension(() -> List.of(excluded), helper::getLevel);
                final var layout = new Layout();
                helper.assertTrue(!extension.isHandled(holder()), "Empty output cycle was shown as craftable");
                extension.setRecipe(holder(), layout.builder, grid(), null);
                helper.assertTrue(layout.slots.isEmpty() && layout.link == null, "Empty cycle produced fake display ingredients");
            });
        }

        private static net.minecraft.world.item.crafting.RecipeHolder<LootDiskCyclingRecipe> holder() {
            return new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("neoopencomputers", "loot_disk_cycling"), new LootDiskCyclingRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC));
        }

        private static mezz.jei.api.gui.ingredient.ICraftingGridHelper grid() {
            try { return (mezz.jei.api.gui.ingredient.ICraftingGridHelper) Class.forName("mezz.jei.library.gui.helpers.CraftingGridHelper").getField("INSTANCE").get(null); }
            catch (ReflectiveOperationException failure) { throw new AssertionError("Pinned JEI crafting helper unavailable", failure); }
        }

        private static void isolated(GameTestHelper helper, boolean bundled, java.util.function.Consumer<li.cil.oc.common.ItemRegistry> test) {
            final var previous = li.cil.oc.api.API.items;
            try {
                final var registry = new li.cil.oc.common.ItemRegistry(); li.cil.oc.api.API.items = registry;
                li.cil.oc.common.ModContentCatalog.registerDefaults();
                if (bundled) li.cil.oc.common.ModLootDisks.registerDefaults();
                test.accept(registry);
            } finally { li.cil.oc.api.API.items = previous; }
            helper.succeed();
        }

        private static final class Slot {
            final java.util.ArrayList<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
            final mezz.jei.api.gui.builder.IRecipeSlotBuilder builder = proxy(mezz.jei.api.gui.builder.IRecipeSlotBuilder.class, (method, args) -> {
                if (method.equals("addIngredients")) {
                    @SuppressWarnings("unchecked") final var stacks = (List<net.minecraft.world.item.ItemStack>) args[1];
                    items.addAll(stacks);
                } else if (!method.equals("setStandardSlotBackground") && !method.equals("setOutputSlotBackground")) throw new AssertionError(method);
                return this.builder;
            });
        }

        private static final class Layout {
            final java.util.ArrayList<Slot> slots = new java.util.ArrayList<>();
            Object[] link;
            final mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder = proxy(mezz.jei.api.gui.builder.IRecipeLayoutBuilder.class, (method, args) -> {
                switch (method) {
                    case "addInputSlot", "addOutputSlot" -> { final var slot = new Slot(); slots.add(slot); return slot.builder; }
                    case "setShapeless" -> { return null; }
                    case "createFocusLink" -> { link = (Object[]) args[0]; return null; }
                    default -> throw new AssertionError(method);
                }
            });
        }

        static void registration(GameTestHelper helper) {
            final var registered = new java.util.concurrent.atomic.AtomicBoolean();
            final var category = proxy(mezz.jei.api.recipe.category.extensions.vanilla.crafting.IExtendableCraftingRecipeCategory.class, (method, args) -> {
                helper.assertTrue(method.equals("addExtension") && args[0] == LootDiskCyclingRecipe.class && args[1] != null, "Wrong crafting recipe extension registration");
                registered.set(true); return null;
            });
            final var registration = proxy(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration.class, (method, args) -> switch (method) {
                case "getCraftingCategory" -> category;
                default -> throw new AssertionError(method);
            });
            new OpenComputersJeiPlugin().registerVanillaCategoryExtensions(registration);
            helper.assertTrue(registered.get(), "JEI cannot display the actual loot disk crafting recipe");
            helper.succeed();
        }

        private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> call) {
            return type.cast(java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> call.apply(method.getName(), args)));
        }
    }
}
