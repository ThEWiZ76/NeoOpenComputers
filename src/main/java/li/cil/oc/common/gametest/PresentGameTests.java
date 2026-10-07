package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class PresentGameTests {
    @GameTest(template = "empty")
    public static void presentDropsGiftWhenInventoryIsFull(GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.getRandom().setSeed(1234);
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64));
        }
        final var present = new ItemStack(li.cil.oc.common.ModItems.PRESENT.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, present);
        final var bounds = player.getBoundingBox().inflate(3);
        final var world = helper.getLevel();
        final var before = java.util.Set.copyOf(world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds));
        present.getItem().use(world, player, InteractionHand.MAIN_HAND);
        final var drops = world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().filter(entity -> !before.contains(entity)).toList();
        try {
            helper.assertTrue(present.getCount() == 1 && drops.size() == 1 && drops.getFirst().getItem().getCount() == 1
                && !drops.getFirst().getItem().is(li.cil.oc.common.ModItems.PRESENT.get()), "Full inventory lost or duplicated the gift");
        } finally {
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void presentsRespectSeasonChanceAndRecraftExclusions(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var player = new net.minecraft.server.level.ServerPlayer(world.getServer(), world,
            new com.mojang.authlib.GameProfile(new java.util.UUID(0, 654), "present_fixture"), net.minecraft.server.level.ClientInformation.createDefault());
        player.getRandom().setSeed(1234);
        final var holiday = java.time.Clock.fixed(java.time.Instant.parse("2026-12-25T12:00:00Z"), java.time.ZoneOffset.UTC);
        final var matrix = new net.minecraft.world.SimpleContainer(9);
        final var analyzer = new ItemStack(li.cil.oc.common.ModItems.ANALYZER.get());
        final var event = new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player, analyzer, matrix);
        final var enabled = new li.cil.oc.common.PresentHandler(holiday, () -> 1);
        enabled.onCraft(event);
        helper.assertTrue(player.getInventory().countItem(li.cil.oc.common.ModItems.PRESENT.get()) == 1, "Holiday OC crafting did not grant one present");
        new li.cil.oc.common.PresentHandler(holiday, () -> 0).onCraft(event);
        new li.cil.oc.common.PresentHandler(java.time.Clock.fixed(java.time.Instant.parse("2026-07-01T12:00:00Z"), java.time.ZoneOffset.UTC), () -> 1).onCraft(event);
        enabled.onCraft(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player, new ItemStack(net.minecraft.world.item.Items.STICK), matrix));
        enabled.onCraft(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player, new ItemStack(li.cil.oc.common.ModItems.NAVIGATION_UPGRADE.get()), matrix));
        helper.assertTrue(player.getInventory().countItem(li.cil.oc.common.ModItems.PRESENT.get()) == 1, "Disabled/off-season/non-OC/recraft output granted a present");
        final var fake = new net.neoforged.neoforge.common.util.FakePlayer(world,
            new com.mojang.authlib.GameProfile(new java.util.UUID(0, 655), "present_fake"));
        enabled.onCraft(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(fake, analyzer, matrix));
        helper.assertTrue(fake.getInventory().countItem(li.cil.oc.common.ModItems.PRESENT.get()) == 0, "Fake player got a crafting present");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void presentWeightsAndDisabledRecipesPreserveGift(GameTestHelper helper) {
        final var entries = li.cil.oc.common.item.PresentLoot.ENTRIES;
        helper.assertTrue(entries.size() == 77 && entries.stream().mapToInt(li.cil.oc.common.item.PresentLoot.Entry::weight).sum() == 5358,
            "Original present loot table changed");
        final var boundaryPool = java.util.List.of(entries.getFirst(), entries.getLast());
        helper.assertTrue(li.cil.oc.common.item.PresentLoot.select(boundaryPool, 519).is(li.cil.oc.common.ModItems.ARROW_KEYS.get())
            && li.cil.oc.common.item.PresentLoot.select(boundaryPool, 520).is(li.cil.oc.common.ModItems.MEMORY_TIER6.get()), "Weighted boundary selected the wrong gift");
        final var recipes = helper.getLevel().getRecipeManager();
        final var original = java.util.List.copyOf(recipes.getRecipes());
        final var arrowRecipe = original.stream().filter(recipe -> recipe.value().getResultItem(helper.getLevel().registryAccess()).is(li.cil.oc.common.ModItems.ARROW_KEYS.get())).findFirst().orElseThrow();
        try {
            recipes.replaceRecipes(java.util.List.of(arrowRecipe));
            final var pool = li.cil.oc.common.item.PresentLoot.eligible(helper.getLevel());
            helper.assertTrue(pool.size() == 1 && pool.getFirst().weight() == 520
                && li.cil.oc.common.item.PresentLoot.next(helper.getLevel(), net.minecraft.util.RandomSource.create(1234)).is(li.cil.oc.common.ModItems.ARROW_KEYS.get()),
                "Disabled recipes still contributed present loot");
            recipes.replaceRecipes(java.util.List.of());
            final var player = helper.makeMockPlayer(GameType.SURVIVAL);
            final var stack = new ItemStack(li.cil.oc.common.ModItems.PRESENT.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(!stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction() && stack.getCount() == 2,
                "Empty loot pool consumed the present");
        } finally {
            recipes.replaceRecipes(original);
        }
        final var tab = li.cil.oc.common.ModCreativeTabs.MAIN.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().noneMatch(stack -> stack.is(li.cil.oc.common.ModItems.PRESENT.get())), "Present was exposed in Creative tab");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void presentOpensIntoOneCraftableGift(GameTestHelper helper) {
        final var registered = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "present"));
        helper.assertTrue(registered.isPresent(), "Present item is missing");
        final var item = registered.orElseThrow();
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getRandom().setSeed(1234);
        final var stack = new ItemStack(item, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction() && stack.getCount() == 1,
            "Opening did not consume exactly one present");
        final var gifts = player.getInventory().items.stream().filter(candidate -> !candidate.isEmpty() && candidate.getItem() != item).toList();
        helper.assertTrue(gifts.size() == 1 && gifts.getFirst().getCount() == 1, "Opening did not deliver exactly one gift");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream().anyMatch(recipe ->
            !recipe.value().getIngredients().isEmpty() && recipe.value().getResultItem(helper.getLevel().registryAccess()).getItem() == gifts.getFirst().getItem()),
            "Gift was not craftable in the current recipe set");
        helper.succeed();
    }
}
