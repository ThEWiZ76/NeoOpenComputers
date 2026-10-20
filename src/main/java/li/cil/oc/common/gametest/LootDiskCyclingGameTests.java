package li.cil.oc.common.gametest;

import li.cil.oc.api.API;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModContentCatalog;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Consumer;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class LootDiskCyclingGameTests {
    @GameTest(template = "empty") public static void addonDisksParticipateInTheRealCycle(GameTestHelper helper) {
        isolated(helper, registry -> {
            final var bundled = ModLootDisks.bundledStacksForCycling();
            final ItemStack first = registry.registerFloppy("Fixture A", DyeColor.BLUE, () -> null, true);
            final ItemStack second = registry.registerFloppy("Fixture B", DyeColor.RED, () -> null, true);
            final var recipe = new LootDiskCyclingRecipe(CraftingBookCategory.MISC);
            helper.assertTrue(cycle(recipe, first, helper).getHoverName().getString().equals("Fixture B"), "Registered addon disk was omitted from cycling outputs");
            helper.assertTrue(cycle(recipe, bundled.getLast(), helper).getHoverName().getString().equals("Fixture A"), "Bundled disks did not advance into registered addon disks");
            helper.assertTrue(ItemStack.isSameItemSameComponents(cycle(recipe, second, helper), bundled.getFirst()), "Last registered disk did not wrap to the first disk");
            first.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Mutated caller"));
            helper.assertTrue(cycle(recipe, bundled.getLast(), helper).getHoverName().getString().equals("Fixture A"), "Caller mutation changed registered cycling template");
        });
    }

    @GameTest(template = "empty") public static void excludedLootDiskIsAnInputButNeverAnOutput(GameTestHelper helper) {
        isolated(helper, registry -> {
            final ItemStack excluded = registry.registerFloppy("Excluded fixture", DyeColor.BLUE, () -> null, false);
            final var recipe = new LootDiskCyclingRecipe(CraftingBookCategory.MISC);
            final var input = input(excluded);
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "doRecipeCycling=false incorrectly rejected a loot-disk input");
            final ItemStack expected = ModLootDisks.bundledStacksForCycling().getFirst();
            helper.assertTrue(ItemStack.isSameItemSameComponents(recipe.assemble(input, helper.getLevel().registryAccess()), expected), "Excluded disk did not recraft into the first eligible disk");
            final var remaining = recipe.getRemainingItems(input);
            helper.assertTrue(remaining.get(0).isEmpty() && remaining.get(1).is(ModItems.WRENCH.get()), "Cycling did not consume only the disk");
        });
    }

    @GameTest(template = "empty") public static void legacyLootFactoryCanBeRecrafted(GameTestHelper helper) {
        isolated(helper, registry -> {
            final var legacy = new ItemStack(ModItems.FLOPPY.get());
            final var data = new CompoundTag(); data.putString("oc:lootFactory", "neoopencomputers:loot/openos");
            legacy.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            final var recipe = new LootDiskCyclingRecipe(CraftingBookCategory.MISC);
            helper.assertTrue(recipe.matches(input(legacy), helper.getLevel()), "Legacy loot-factory disk was rejected");
            final var result = cycle(recipe, legacy, helper).get(DataComponents.CUSTOM_DATA);
            helper.assertTrue(result != null && result.copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG).equals("neoopencomputers:loot/oppm"), "Legacy factory identity did not advance to the correct disk");
        });
    }

    @GameTest(template = "empty") public static void unknownAndEmptyFactoriesUseFirstEligibleDisk(GameTestHelper helper) {
        isolated(helper, registry -> {
            final var recipe = new LootDiskCyclingRecipe(CraftingBookCategory.MISC);
            final var expected = ModLootDisks.stacksForCycling().getFirst();
            for (final String factory : List.of("", "missing:fixed-fixture")) {
                final var disk = new ItemStack(ModItems.FLOPPY.get());
                final var tag = new CompoundTag(); tag.putString(ItemRegistry.FLOPPY_FACTORY_ID_TAG, factory);
                disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                helper.assertTrue(recipe.matches(input(disk), helper.getLevel()), "Present string factory was rejected");
                helper.assertTrue(ItemStack.isSameItemSameComponents(cycle(recipe, disk, helper), expected), "Unknown/empty factory did not use first eligible disk");
            }
            helper.assertTrue(!recipe.matches(input(new ItemStack(ModItems.FLOPPY.get())), helper.getLevel()), "Ordinary writable floppy became a loot-disk input");
            final var snapshot = ModLootDisks.stacksForCycling(); snapshot.getFirst().setCount(0);
            helper.assertTrue(!ModLootDisks.stacksForCycling().getFirst().isEmpty(), "Caller mutated the registered cycling snapshot");
        });
    }

    @GameTest(template = "empty") public static void recraftingUsesTheFactoryMountedByTheDiskDriver(GameTestHelper helper) {
        isolated(helper, registry -> {
            final var firstCalls = new java.util.concurrent.atomic.AtomicInteger();
            final var secondCalls = new java.util.concurrent.atomic.AtomicInteger();
            final var first = registry.registerFloppy("precedence-first", DyeColor.BLUE, () -> {
                firstCalls.incrementAndGet(); return li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromMemory(64));
            }, true);
            final var second = registry.registerFloppy("precedence-second", DyeColor.RED, () -> {
                secondCalls.incrementAndGet(); return li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromMemory(64));
            }, true);
            final var disk = second.copy();
            final var tag = disk.get(DataComponents.CUSTOM_DATA).copyTag();
            tag.putString("oc:lootFactory", first.get(DataComponents.CUSTOM_DATA).copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG));
            disk.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            final var environment = li.cil.oc.api.Driver.driverFor(disk).createEnvironment(disk, null);
            helper.assertTrue(environment != null && firstCalls.get() == 1 && secondCalls.get() == 0, "Disk driver did not select the legacy factory first");
            try {
                final var result = cycle(new LootDiskCyclingRecipe(CraftingBookCategory.MISC), disk, helper);
                helper.assertTrue(ItemStack.isSameItemSameComponents(result, second), "Recrafting advanced from a different factory than the mounted disk");
            } finally { environment.node().remove(); }
        });
    }

    private static CraftingInput input(ItemStack disk) {
        return CraftingInput.of(2, 1, List.of(disk, new ItemStack(ModItems.WRENCH.get())));
    }
    private static ItemStack cycle(LootDiskCyclingRecipe recipe, ItemStack disk, GameTestHelper helper) {
        return recipe.assemble(input(disk), helper.getLevel().registryAccess());
    }
    private static void isolated(GameTestHelper helper, Consumer<ItemRegistry> test) {
        final var previous = API.items;
        try {
            final var registry = new ItemRegistry(); API.items = registry;
            ModContentCatalog.registerDefaults(); ModLootDisks.registerDefaults();
            test.accept(registry);
        } finally { API.items = previous; }
        helper.succeed();
    }
}
