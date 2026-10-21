package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.item.NavigationUpgradeItem;
import li.cil.oc.common.item.ServerItem;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DisassemblerBuiltinItemGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void builtinServerPlansReturnHardwareForEveryTierAndLegacyStorage(GameTestHelper helper) {
        for (var item : List.of(ModItems.SERVER_TIER1.get(), ModItems.SERVER_TIER2.get(), ModItems.SERVER_TIER3.get())) {
            for (boolean legacy : List.of(false, true)) {
                final var memory = enchanted(helper, new ItemStack(ModItems.MEMORY_TIER1.get()));
                final var server = server(helper, new ItemStack(item), memory, legacy);
                final var before = server.get(DataComponents.CUSTOM_DATA).copyTag();
                final var template = DisassemblerTemplates.select(server);
                helper.assertTrue(template.isPresent(), "No builtin server disassembler template");
                final var result = template.orElseThrow().plan(server, new ItemStack[]{new ItemStack(Items.IRON_INGOT)}, helper.getLevel().registryAccess());
                helper.assertTrue(result.queued().length == 1 && result.queued()[0].is(Items.IRON_INGOT)
                    && result.immediate().length == 1 && ItemStack.matches(result.immediate()[0], memory),
                    "Server template mixed paid materials and installed registry-bound hardware");
                helper.assertTrue(before.equals(server.get(DataComponents.CUSTOM_DATA).copyTag()), "Server preview mutated/migrated caller data");
            }
        }
        final var server = new ItemStack(ModItems.SERVER_TIER3.get());
        final var entries = new ListTag();
        final int slotCount = ServerRackMountableEnvironment.slotCountForTier(2);
        for (int slot = 0; slot < slotCount; slot++) {
            final var part = switch (ServerRackMountableEnvironment.slotTypeName(2, slot)) {
                case li.cil.oc.api.driver.item.Slot.Card -> new ItemStack(ModItems.NETWORK_CARD.get());
                case li.cil.oc.api.driver.item.Slot.CPU -> new ItemStack(ModItems.CPU_TIER3.get());
                case li.cil.oc.api.driver.item.Slot.ComponentBus -> new ItemStack(ModItems.COMPONENT_BUS_TIER3.get());
                case li.cil.oc.api.driver.item.Slot.Memory -> new ItemStack(ModItems.MEMORY_TIER6.get());
                case li.cil.oc.api.driver.item.Slot.HDD -> new ItemStack(ModItems.HDD_TIER3.get());
                default -> RobotMovementPersistenceGameTests.eeprom("return");
            };
            final var entry = (CompoundTag) enchanted(helper, part).save(helper.getLevel().registryAccess());
            entry.putByte("Slot", (byte) slot); entries.add(entry);
        }
        final var data = new CompoundTag(); data.put("Items", entries); server.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        final var full = DisassemblerTemplates.select(server).orElseThrow().plan(server, new ItemStack[0], helper.getLevel().registryAccess());
        helper.assertTrue(slotCount > 9 && full.immediate().length == slotCount,
            "Full server inventory was truncated to disassembler internal output capacity");
        for (var part : full.immediate()) helper.assertTrue(part.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("retained") == 42
            && !part.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(), "Full server inventory lost component metadata");
        final var broken = new CompoundTag(); broken.putString("id", "missing:component"); broken.putByte("Slot", (byte) 0);
        final var brokenEntries = new ListTag(); brokenEntries.add(broken); data.put("Items", brokenEntries);
        server.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        helper.assertTrue(DisassemblerTemplates.select(server).orElseThrow().plan(server, new ItemStack[]{new ItemStack(Items.IRON_INGOT)}, helper.getLevel().registryAccess()).isEmpty(),
            "Unreadable installed server hardware was discarded instead of blocking disassembly");
        helper.setBlock(POS, ModBlocks.DISASSEMBLER.get());
        final DisassemblerBlockEntity machine = helper.getBlockEntity(POS); machine.setItem(0, server.copy());
        helper.assertTrue(!machine.disassemble(RandomSource.create(0L), 0D) && ItemStack.matches(machine.getItem(0), server),
            "Physical disassembler consumed a server with unreadable hardware");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void builtinServerHardwareIsImmediateAndEscapesRecipeLoss(GameTestHelper helper) throws Exception {
        final var manager = helper.getLevel().getRecipeManager(); final var original = List.copyOf(manager.getRecipes());
        final var output = new ItemStack(ModItems.SERVER_TIER1.get());
        final var recipe = new ShapelessRecipe("", CraftingBookCategory.MISC, output,
            NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.IRON_INGOT)));
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "test_server_disassembly"), recipe)));
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 1D, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 4D, () -> {
                    final var memory = enchanted(helper, new ItemStack(ModItems.MEMORY_TIER1.get()));
                    helper.setBlock(POS, ModBlocks.DISASSEMBLER.get()); helper.setBlock(POS.below(), Blocks.CHEST);
                    final DisassemblerBlockEntity machine = helper.getBlockEntity(POS);
                    final Container chest = helper.getBlockEntity(POS.below());
                    machine.setItem(0, server(helper, output.copy(), memory, false));
                    helper.assertTrue(machine.disassemble() && count(chest, memory) == 1 && count(chest, new ItemStack(Items.IRON_INGOT)) == 0,
                        "Server contents were lost or made dependent on recipe energy");
                    helper.assertTrue(!machine.disassemble() && count(chest, memory) == 1, "Busy server disassembly repeated immediate contents");
                    ((Connector) machine.node()).changeBuffer(4D);
                    for (int tick = 0; tick < 8; tick++) DisassemblerBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
                    helper.assertTrue(count(chest, memory) == 1 && count(chest, new ItemStack(Items.IRON_INGOT)) == 0,
                        "Server contents were repeated/lost or ordinary materials escaped guaranteed loss");
                }));
        } finally { manager.replaceRecipes(original); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void navigationRecipeAndDisassemblyRetainOneOriginalMap(GameTestHelper helper) {
        final var map = enchanted(helper, new ItemStack(Items.FILLED_MAP, 2));
        map.set(DataComponents.MAP_ID, new MapId(731));
        map.set(DataComponents.CUSTOM_NAME, Component.literal("OC retained map"));
        final var input = CraftingInput.of(3, 3, List.of(
            new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.COMPASS), new ItemStack(Items.GOLD_INGOT),
            new ItemStack(ModItems.MICROCHIP_TIER2.get()), map, new ItemStack(ModItems.MICROCHIP_TIER2.get()),
            new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.POTION), new ItemStack(Items.GOLD_INGOT)));
        final var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "Navigation test recipe did not match");
        final var upgrade = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        // Saving the running driver's data must not erase the separately retained map item.
        final var driver = ModItems.NAVIGATION_UPGRADE.get().createEnvironment(upgrade, null);
        driver.save(new CompoundTag());
        final var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) upgrade.save(helper.getLevel().registryAccess()));
        helper.setBlock(POS, ModBlocks.DISASSEMBLER.get());
        final DisassemblerBlockEntity machine = helper.getBlockEntity(POS); machine.setItem(0, restored);
        helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D), "Crafted navigation upgrade could not be disassembled");
        helper.assertTrue(count(machine, map.copyWithCount(1)) == 1 && map.getCount() == 2,
            "Navigation recovery lost map metadata, duplicated its source stack or mutated crafting input");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void navigationLegacyMapIdIsRecoveredWithoutMutatingTheUpgrade(GameTestHelper helper) {
        final var upgrade = new ItemStack(ModItems.NAVIGATION_UPGRADE.get());
        NavigationUpgradeItem.storeMapData(upgrade, new li.cil.oc.common.component.NavigationUpgradeEnvironment.NavigationMapData(128, -128, 2, 731));
        final var before = upgrade.get(DataComponents.CUSTOM_DATA).copyTag();
        final var template = DisassemblerTemplates.select(upgrade);
        helper.assertTrue(template.isPresent(), "No builtin navigation template");
        final var result = template.orElseThrow().plan(upgrade, new ItemStack[]{new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.FILLED_MAP)}, helper.getLevel().registryAccess());
        helper.assertTrue(result.queued().length == 2 && result.queued()[1].is(Items.FILLED_MAP)
            && new MapId(731).equals(result.queued()[1].get(DataComponents.MAP_ID)) && result.immediate().length == 0,
            "Existing port navigation map ID was lost or moved outside the paid queue");
        helper.assertTrue(before.equals(upgrade.get(DataComponents.CUSTOM_DATA).copyTag()), "Navigation preview mutated caller data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptNavigationSnapshotCannotConsumeTheUpgrade(GameTestHelper helper) {
        final var upgrade = new ItemStack(ModItems.NAVIGATION_UPGRADE.get());
        final var root = new CompoundTag(); final var broken = new CompoundTag(); broken.putString("id", "missing:map");
        root.put("oc:navigationMap", broken); upgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        final var template = DisassemblerTemplates.select(upgrade);
        helper.assertTrue(template.isPresent(), "No builtin navigation template");
        helper.assertTrue(template.orElseThrow().plan(upgrade, new ItemStack[]{new ItemStack(Items.FILLED_MAP)}, helper.getLevel().registryAccess()).isEmpty(),
            "Corrupt retained navigation map was silently replaced by a different map");
        helper.assertTrue(root.equals(upgrade.get(DataComponents.CUSTOM_DATA).copyTag()), "Corrupt navigation preview mutated its input");
        helper.setBlock(POS, ModBlocks.DISASSEMBLER.get());
        final DisassemblerBlockEntity machine = helper.getBlockEntity(POS); machine.setItem(0, upgrade.copy());
        helper.assertTrue(!machine.disassemble(RandomSource.create(0L), 0D) && ItemStack.matches(machine.getItem(0), upgrade),
            "Physical disassembler consumed a navigation upgrade with an unreadable map");
        helper.succeed();
    }

    private static ItemStack server(GameTestHelper helper, ItemStack server, ItemStack memory, boolean legacy) {
        final int tier = ((ServerItem) server.getItem()).tier();
        int memorySlot = 0;
        while (!li.cil.oc.api.driver.item.Slot.Memory.equals(ServerRackMountableEnvironment.slotTypeName(tier, memorySlot))) memorySlot++;
        final var entry = (CompoundTag) memory.save(helper.getLevel().registryAccess()); entry.putByte("Slot", (byte) memorySlot);
        final var entries = new ListTag(); entries.add(entry); final var data = new CompoundTag(); data.put("Items", entries);
        final var root = new CompoundTag(); if (legacy) root.put("oc:rackMountable", data); else root.merge(data);
        server.set(DataComponents.CUSTOM_DATA, CustomData.of(root)); return server;
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack) {
        final var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        final var tag = new CompoundTag(); tag.putInt("retained", 42); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack;
    }

    private static int count(Container inventory, ItemStack expected) {
        int count = 0;
        for (int slot = inventory instanceof DisassemblerBlockEntity ? 1 : 0; slot < inventory.getContainerSize(); slot++) {
            final var stack = inventory.getItem(slot); if (ItemStack.isSameItemSameComponents(stack, expected)) count += stack.getCount();
        }
        return count;
    }
}
