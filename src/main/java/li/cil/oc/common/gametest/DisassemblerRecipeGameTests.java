package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DisassemblerRecipeGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void disassemblerRecoversOrdinaryShapelessOcIngredients(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("simple", cpu(), Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.REDSTONE))), () -> {
            final var machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.canDisassemble() && machine.disassemble(RandomSource.create(0L), 0D), "Ordinary OC crafting recipe was not recoverable");
            helper.assertTrue(machine.getItem(0).isEmpty() && count(machine, Items.IRON_INGOT) == 1 && count(machine, Items.REDSTONE) == 1, "Recipe disassembly lost or duplicated ingredients");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRecoversShapedRepeatedIngredients(GameTestHelper helper) throws Exception {
        final var pattern = ShapedRecipePattern.of(Map.of('I', Ingredient.of(Items.IRON_INGOT), 'R', Ingredient.of(Items.REDSTONE)), "II", "IR");
        final RecipeHolder<?> recipe = new RecipeHolder<>(id("shaped"), new ShapedRecipe("", CraftingBookCategory.MISC, pattern, cpu()));
        recipes(helper, List.of(recipe), () -> {
            final var machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.IRON_INGOT) == 3 && count(machine, Items.REDSTONE) == 1, "Shaped/repeated ingredient counts were not recovered");
            helper.assertTrue(pattern.ingredients().stream().filter(ingredient -> !ingredient.isEmpty()).allMatch(ingredient -> ingredient.getItems()[0].getCount() == 1), "Disassembly mutated shared recipe ingredients");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRestrictsOrdinaryForeignRecipesByDefault(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("foreign", new ItemStack(Items.DIAMOND), Ingredient.of(Items.EMERALD))), () -> {
            final var machine = machine(helper); final var stack = new ItemStack(Items.DIAMOND);
            helper.assertTrue(!machine.canInsert(stack), "Default recipe fallback accepted an unregistered foreign item");
            machine.setItem(0, stack);
            helper.assertTrue(!machine.disassemble(RandomSource.create(0L), 0D) && machine.getItem(0).is(Items.DIAMOND), "Rejected foreign input was consumed");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerCanOptIntoOrdinaryForeignRecipes(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("foreign_optin", new ItemStack(Items.DIAMOND), Ingredient.of(Items.EMERALD))), () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLE_ALL_THE_THINGS, true, () -> {
                final var machine = machine(helper);
                helper.assertTrue(machine.canInsert(new ItemStack(Items.DIAMOND)), "Opt-in did not enable ordinary foreign recipe recovery");
                machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.EMERALD) == 1, "Foreign recipe output count differs");
            })); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerBlacklistSkipsAForbiddenRecipe(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("a_forbidden", cpu(), Ingredient.of(Items.IRON_INGOT)), shapeless("z_allowed", cpu(), Ingredient.of(Items.COPPER_INGOT))), () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_INPUT_BLACKLIST, List.of("minecraft:iron_ingot"), () -> {
                final var machine = machine(helper); machine.setItem(0, cpu());
                helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.IRON_INGOT) == 0 && count(machine, Items.COPPER_INGOT) == 1, "Blacklist did not skip the forbidden candidate for its valid alternative");
            })); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerExcludesFluidBucketsFromRecoveredInputs(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("bucket", cpu(), Ingredient.of(Items.LAVA_BUCKET), Ingredient.of(Items.DIAMOND))), () -> {
            final var machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.DIAMOND) == 1
                && count(machine, Items.LAVA_BUCKET) == 0 && count(machine, Items.BUCKET) == 0, "Disassembly returned a fluid/container which was already returned during crafting");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRejectsSelfIngredientFeedback(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("feedback", cpu(), Ingredient.of(ModItems.CPU_TIER1.get()), Ingredient.of(Items.IRON_INGOT))), () -> {
            final var machine = machine(helper);
            helper.assertTrue(!machine.canInsert(cpu()), "Self-ingredient recipe admitted a positive feedback loop"); machine.setItem(0, cpu());
            helper.assertTrue(!machine.disassemble() && machine.getItem(0).is(ModItems.CPU_TIER1.get()), "Self-ingredient rejection destroyed input");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRejectsFractionalInputsBeforeMerging(GameTestHelper helper) throws Exception {
        final var output = cpu(); output.setCount(2);
        recipes(helper, List.of(shapeless("fractional", output, Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.IRON_INGOT))), () -> {
            final var machine = machine(helper);
            helper.assertTrue(!machine.canInsert(cpu()), "Fractional per-input recipe recovery differs from upstream pre-merge filtering"); machine.setItem(0, cpu());
            helper.assertTrue(!machine.disassemble() && machine.getItem(0).getCount() == 1, "Fractional recipe rejection lost input");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerResolvesAlternativesWithoutMutatingTheRecipe(GameTestHelper helper) throws Exception {
        final var alternatives = Ingredient.of(Items.EMERALD, Items.DIAMOND);
        final Item first = alternatives.getItems()[0].getItem();
        recipes(helper, List.of(shapeless("alternatives", cpu(), alternatives)), () -> {
            final var machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.DIAMOND) == 1 && count(machine, Items.EMERALD) == 0, "Stable registry-ordered ingredient alternative was not selected");
            helper.assertTrue(alternatives.getItems()[0].getItem() == first && alternatives.getItems()[0].getCount() == 1, "Alternative resolution reordered/mutated the source ingredient");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerMatchesUsedOcItemWithoutLeakingItsData(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("used", cpu(), Ingredient.of(Items.IRON_INGOT))), () -> {
            final var input = cpu(); final var tag = new CompoundTag(); tag.putInt("retained", 42); input.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            final var machine = machine(helper);
            helper.assertTrue(machine.canInsert(input), "Runtime custom data prevented ordinary OC recipe matching"); machine.setItem(0, input);
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.IRON_INGOT) == 1, "Used OC item did not recover its recipe ingredient");
            helper.assertTrue(machine.getItem(1).get(DataComponents.CUSTOM_DATA) == null, "Runtime host data was copied onto unrelated recipe ingredients");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerKeepsExplicitTemplatePriority(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("priority", cpu(), Ingredient.of(Items.IRON_INGOT))), () -> {
            try (var ignored = DisassemblerTemplates.register(new DisassemblerTemplate() {
                public String name() { return "recipe_priority_probe"; }
                public boolean matches(ItemStack stack) { return stack.is(ModItems.CPU_TIER1.get()); }
                public ItemStack[] disassemble(ItemStack stack) { return new ItemStack[]{new ItemStack(Items.EMERALD)}; }
            })) {
                final var machine = machine(helper); machine.setItem(0, cpu());
                helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.EMERALD) == 1 && count(machine, Items.IRON_INGOT) == 0, "Ordinary recipe fallback replaced an explicitly registered template");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerSeesChangedAndRemovedRecipesImmediately(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("reload", cpu(), Ingredient.of(Items.IRON_INGOT))), () -> {
            var machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.IRON_INGOT) == 1, "Initial recipe was not recovered");
            helper.getLevel().getRecipeManager().replaceRecipes(List.of(shapeless("reload", cpu(), Ingredient.of(Items.COPPER_INGOT))));
            machine = machine(helper); machine.setItem(0, cpu());
            helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(machine, Items.COPPER_INGOT) == 1 && count(machine, Items.IRON_INGOT) == 0, "Same-ID recipe replacement retained cached ingredients");
            helper.getLevel().getRecipeManager().replaceRecipes(List.of()); machine = machine(helper);
            helper.assertTrue(!machine.canInsert(cpu()), "Removed recipe stayed available through the recovery index");
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerIgnoresNonCraftingRecipeOutputs(GameTestHelper helper) throws Exception {
        final RecipeHolder<?> recipe = new RecipeHolder<>(id("smelting"), new SmeltingRecipe("", CookingBookCategory.MISC, Ingredient.of(Items.IRON_INGOT), cpu(), 0F, 20));
        recipes(helper, List.of(recipe), () -> helper.assertTrue(!machine(helper).canInsert(cpu()), "A smelting recipe was reversed as if it were ordinary crafting")); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerDoesNotRestoreDamagedToolsFromAnUndamagedRecipe(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("damaged", new ItemStack(Items.DIAMOND_PICKAXE), Ingredient.of(Items.DIAMOND))), () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLE_ALL_THE_THINGS, true, () -> {
                final var stack = new ItemStack(Items.DIAMOND_PICKAXE); stack.setDamageValue(1);
                helper.assertTrue(!machine(helper).canInsert(stack), "Damaged-tool identity was ignored while reversing a recipe");
            })); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerKeepsACommittedRecipeJobAfterRecipeRemovalAndReload(GameTestHelper helper) throws Exception {
        recipes(helper, List.of(shapeless("queued", cpu(), Ingredient.of(Items.IRON_INGOT))), () -> {
            final var machine = machine(helper); machine.setItem(0, cpu()); tick(helper, machine);
            helper.assertTrue(machine.getItem(0).isEmpty(), "Generic recipe did not automatically queue");
            ((Connector) machine.node()).changeBuffer(2); tick(helper, machine);
            final var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess()); machine.onChunkUnloaded(); machine.setRemoved();
            helper.getLevel().getRecipeManager().replaceRecipes(List.of());
            final var restored = (DisassemblerBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(restored); restored.onLoad();
            ((Connector) restored.node()).changeBuffer(2); tick(helper, restored); tick(helper, restored); tick(helper, restored);
            helper.assertTrue(count(restored, Items.IRON_INGOT) == 1 && ((Connector) restored.node()).globalBuffer() == 0D
                && restored.saveWithFullMetadata(helper.getLevel().registryAccess()).getList("oc:queue", 10).isEmpty(),
                "Saved partial payment/job was recalculated/lost/duplicated when its recipe disappeared");
            helper.assertTrue(!restored.canInsert(cpu()), "Removed recipe became available again after job reload");
        }); helper.succeed();
    }

    private static void recipes(GameTestHelper helper, List<RecipeHolder<?>> replacements, NeoOpenComputersGameTests.ThrowingRunnable action) throws Exception {
        final var manager = helper.getLevel().getRecipeManager(); final var original = List.copyOf(manager.getRecipes());
        try {
            manager.replaceRecipes(replacements);
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLE_ALL_THE_THINGS, false, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_INPUT_BLACKLIST, List.of("minecraft:fire"), () ->
                    NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0D, () ->
                        NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 4D, () ->
                            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_TICK_AMOUNT, 2D, action)))));
        } finally { manager.replaceRecipes(original); }
    }
    private static RecipeHolder<?> shapeless(String name, ItemStack output, Ingredient... inputs) {
        final var ingredients = NonNullList.withSize(inputs.length, Ingredient.EMPTY);
        for (int i = 0; i < inputs.length; i++) ingredients.set(i, inputs[i]);
        return new RecipeHolder<>(id(name), new ShapelessRecipe("", CraftingBookCategory.MISC, output, ingredients));
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("neoopencomputers", "test_disassembly_" + name); }
    private static ItemStack cpu() { return new ItemStack(ModItems.CPU_TIER1.get()); }
    private static DisassemblerBlockEntity machine(GameTestHelper helper) { helper.setBlock(POS, Blocks.AIR); helper.setBlock(POS, ModBlocks.DISASSEMBLER.get()); return helper.getBlockEntity(POS); }
    private static int count(DisassemblerBlockEntity machine, Item item) { int count = 0; for (int slot = 1; slot < machine.getContainerSize(); slot++) if (machine.getItem(slot).is(item)) count += machine.getItem(slot).getCount(); return count; }
    private static void tick(GameTestHelper helper, DisassemblerBlockEntity machine) { DisassemblerBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine); }
}
