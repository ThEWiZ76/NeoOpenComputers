package li.cil.oc.client.integration.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class JeiCategoryOrderGameTests {
    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return List.of();
        return List.of(new TestFunction("jeiCategoryOrder", "jei_manual_precedes_callbacks_by_default", "neoopencomputers:empty", 100, 0, true, Loaded::order));
    }

    private static final class Loaded {
        static void order(GameTestHelper helper) {
            Path directory = null;
            try {
                directory = Files.createTempDirectory("oc-jei-category-order-");
                final var path = directory.resolve("order.ini");
                final var manual = OpenComputersJeiPlugin.MANUAL;
                final var callbacks = OpenComputersJeiPlugin.CALLBACKS;
                final var foreignA = RecipeType.create("fixture", "a", Object.class);
                final var foreignB = RecipeType.create("fixture", "b", Object.class);
                final List<RecipeType<?>> types = List.of(callbacks, foreignA, manual, foreignB, RecipeTypes.CRAFTING);
                final var fresh = sorter(path, types);
                helper.assertTrue(fresh.compare(manual, callbacks) < 0, "JEI defaults put callback documentation before Manual");
                helper.assertTrue(fresh.compare(RecipeTypes.CRAFTING, manual) < 0, "Manual displaced vanilla crafting priority");

                Files.write(path, List.of(foreignB.getUid().toString(), "neoopencomputers:api", manual.getUid().toString(), foreignA.getUid().toString(), RecipeTypes.CRAFTING.getUid().toString()));
                final var previous = sorter(path, types);
                helper.assertTrue(previous.compare(manual, callbacks) < 0, "Existing automatic api/manual order defeated the corrected category ID");
                helper.assertTrue(previous.compare(foreignB, foreignA) < 0 && previous.compare(foreignA, RecipeTypes.CRAFTING) < 0, "JEI lost unrelated configured category priorities");

                final var preference = List.of(callbacks.getUid().toString(), foreignB.getUid().toString(), manual.getUid().toString(), foreignA.getUid().toString(), RecipeTypes.CRAFTING.getUid().toString());
                Files.write(path, preference);
                final var configured = sorter(path, types);
                helper.assertTrue(configured.compare(callbacks, manual) < 0, "Plugin overrode the user's explicit callback-first preference");
                helper.assertTrue(Files.readAllLines(path).equals(preference), "Already valid user category order was rewritten");
            } catch (IOException | ReflectiveOperationException failure) {
                throw new RuntimeException(failure);
            } finally {
                if (directory != null) {
                    try {
                        Files.deleteIfExists(directory.resolve("order.ini"));
                        Files.delete(directory);
                    } catch (IOException failure) {
                        throw new UncheckedIOException(failure);
                    }
                }
            }
            helper.succeed();
        }

        @SuppressWarnings("unchecked")
        private static Comparator<RecipeType<?>> sorter(Path path, List<RecipeType<?>> types) throws ReflectiveOperationException {
            final var implementation = Class.forName("mezz.jei.library.config.RecipeCategorySortingConfig");
            final var config = implementation.getConstructor(Path.class).newInstance(path);
            return (Comparator<RecipeType<?>>) implementation.getMethod("getComparator", Collection.class).invoke(config, types);
        }
    }
}
