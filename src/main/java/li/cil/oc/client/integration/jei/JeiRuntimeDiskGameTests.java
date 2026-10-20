package li.cil.oc.client.integration.jei;

import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.network.LootDiskClientState;
import li.cil.oc.common.network.LootDiskMetadataPayload;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@GameTestHolder("neoopencomputers")
public final class JeiRuntimeDiskGameTests {
    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return List.of();
        return List.of(new TestFunction("jeiRuntimeDisks", "jei_runtime_disks_batch_and_replace", "neoopencomputers:empty", 100, 0, true, Loaded::runtime));
    }

    private static final class Loaded {
        static void runtime(GameTestHelper helper) {
            final var previous = LootDiskClientState.stacks();
            try {
                LootDiskClientState.reset();
                final var first = ModLootDisks.bundledStacksForCycling().getFirst();
                final var second = ModLootDisks.bundledStacksForCycling().get(1);
                LootDiskClientState.accept(new LootDiskMetadataPayload(first));
                LootDiskClientState.flush();
                final var ingredients = ingredients(); ingredients.add(first.copy());
                final var additions = new ArrayList<List<ItemStack>>();
                try (final var subscription = OpenComputersJeiPlugin.subscribeRuntimeDisks(() -> ingredients, added -> {
                    additions.add(added.stream().map(ItemStack::copy).toList()); ingredients.addAll(added);
                })) {
                    LootDiskClientState.flush();
                    helper.assertTrue(additions.isEmpty(), "Existing JEI ingredients were added twice after a late runtime attachment");
                    second.set(DataComponents.CUSTOM_NAME, Component.literal("Server metadata"));
                    final var data = second.get(DataComponents.CUSTOM_DATA).copyTag();
                    data.putBoolean(li.cil.oc.common.ItemRegistry.FLOPPY_RECIPE_CYCLING_TAG, false);
                    second.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
                    for (int index = 0; index < 4; index++) LootDiskClientState.accept(new LootDiskMetadataPayload(second));
                    LootDiskClientState.flush(); LootDiskClientState.flush();
                    helper.assertTrue(additions.size() == 1 && additions.getFirst().size() == 1 && ingredients.size() == 2, "Runtime did not batch/deduplicate a new server ingredient");
                    second.set(DataComponents.CUSTOM_NAME, Component.literal("Changed metadata"));
                    LootDiskClientState.accept(new LootDiskMetadataPayload(second)); LootDiskClientState.flush();
                    helper.assertTrue(ingredients.size() == 2 && ingredients.stream().anyMatch(stack -> stack.getHoverName().getString().equals("Changed metadata")), "Actual JEI UID store duplicated or lost replacement metadata");
                    additions.getFirst().getFirst().setCount(0);
                    helper.assertTrue(!LootDiskClientState.stacks().get(1).isEmpty() && !first.isEmpty(), "Runtime addition mutated a foreign/private stack");
                    LootDiskClientState.accept(new LootDiskMetadataPayload(first));
                }
                final var replacement = ingredients();
                try (final var subscription = OpenComputersJeiPlugin.subscribeRuntimeDisks(() -> replacement, replacement::addAll)) {
                    LootDiskClientState.flush();
                    helper.assertTrue(replacement.size() == 2 && additions.size() == 2, "Runtime replacement lost existing metadata or notified the detached manager");
                }
            } catch (Exception failure) { throw new RuntimeException(failure); }
            finally {
                LootDiskClientState.reset(); previous.forEach(stack -> LootDiskClientState.accept(new LootDiskMetadataPayload(stack)));
            }
            helper.succeed();
        }

        @SuppressWarnings("unchecked")
        private static Set<ItemStack> ingredients() throws ReflectiveOperationException {
            final var helper = (IIngredientHelper<ItemStack>) Proxy.newProxyInstance(JeiRuntimeDiskGameTests.class.getClassLoader(), new Class<?>[]{IIngredientHelper.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getUid" -> ItemSubtypes.FLOPPY.getSubtypeData((ItemStack) args[0], (UidContext) args[1]);
                case "getIngredientType" -> VanillaTypes.ITEM_STACK;
                case "getErrorInfo" -> "runtime disk fixture";
                default -> throw new UnsupportedOperationException(method.getName());
            });
            return (Set<ItemStack>) Class.forName("mezz.jei.library.ingredients.IngredientSet")
                .getConstructor(IIngredientHelper.class, UidContext.class).newInstance(helper, UidContext.Ingredient);
        }
    }
}
