package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.IMC;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplateImc;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.InterModComms;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class DisassemblerImcGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void disassemblerUnderstandsLegacyQueuedAndImmediateReturnForms(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            for (String method : List.of("bothArrays", "singleQueue", "singleDrop", "flatQueue")) {
                try (var ignored = register("selectDiamond", method)) {
                    final var machine = machine(helper, true); final var chest = chest(helper); machine.setItem(0, new ItemStack(Items.DIAMOND));
                    helper.assertTrue(machine.canDisassemble() && count(chest, Items.PAPER) == 0, "Readiness released immediate contents");
                    helper.assertTrue(machine.disassemble() && machine.getItem(0).isEmpty(), "Legacy two-argument callback did not start");
                    helper.assertTrue(count(chest, Items.PAPER) == (method.equals("flatQueue") ? 0 : 1) && count(chest, Items.EMERALD) == 0,
                        "Immediate contents were lost or queued recipe parts bypassed energy: " + method);
                    ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine); tick(helper, machine);
                    helper.assertTrue(count(chest, Items.EMERALD) == 1 && count(chest, Items.PAPER) == (method.equals("flatQueue") ? 0 : 1),
                        "Legacy queued/immediate outputs differ after paid completion: " + method);
                }
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerPrefersTheLegacySignatureWhenBothExist(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "bothSignatures")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble() && count(chest(helper), Items.PAPER) == 1, "One-argument overload replaced the original ingredient-aware signature");
                ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(count(chest(helper), Items.EMERALD) == 1 && count(chest(helper), Items.IRON_INGOT) == 0, "Legacy callback did not receive the actual recipe ingredient");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerCanReturnOnlyImmediateContents(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "onlyDrops")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble() && machine.getItem(0).isEmpty() && count(chest(helper), Items.PAPER) == 1,
                    "Valid immediate-only disassembly was rejected or lost its contents");
                for (int i = 0; i < 4; i++) tick(helper, machine);
                helper.assertTrue(count(chest(helper), Items.PAPER) == 1 && queue(helper, machine) == 0 && ((Connector) machine.node()).globalBuffer() == 0D,
                    "Immediate-only disassembly repeated or consumed energy");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerDoesNotApplyRecipeLossToImmediateContents(GameTestHelper helper) throws Exception {
        context(helper, () -> NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 1D, () -> {
            try (var ignored = register("selectDiamond", "bothArrays")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble() && count(chest(helper), Items.PAPER) == 1, "Immediate contents were subjected to recipe loss");
                ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(count(chest(helper), Items.PAPER) == 1 && count(chest(helper), Items.EMERALD) == 0 && queue(helper, machine) == 0,
                    "Guaranteed recipe loss failed or destroyed guaranteed immediate contents");
            }
        })); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerIsolatesMalformedImcRegistrations(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            final var bad = payload("selectDiamond", "methodWhichDoesNotExist");
            final var good = payload("selectDiamond", "oneArgument");
            final var registrations = DisassemblerTemplateImc.process(java.util.stream.Stream.of(message(bad), message(good)));
            try {
                helper.assertTrue(registrations.size() == 1, "Malformed registration prevented or duplicated a later healthy template");
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D) && count(chest(helper), Items.EMERALD) == 1, "Healthy IMC registration stopped working after malformed message");
            } finally { registrations.forEach(DisassemblerTemplates.Registration::close); }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerCallbackFailurePreservesItsInput(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "ordinaryFailure")) {
                final var machine = machine(helper, true); final var input = marked(Items.DIAMOND, 1); machine.setItem(0, input);
                helper.assertTrue(!machine.disassemble() && machine.getItem(0).getCount() == 1 && machine.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag().getInt("retained") == 42,
                    "Ordinary callback failure consumed or changed input");
                helper.assertTrue(queue(helper, machine) == 0 && count(chest(helper), Items.PAPER) == 0 && count(chest(helper), Items.EMERALD) == 0, "Failed callback published outputs");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerDoesNotSwallowFatalCallbackErrors(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "fatalFailure")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                boolean propagated = false;
                try { machine.disassemble(); }
                catch (LinkageError error) { propagated = "oc-imc-fatal-probe".equals(error.getMessage()); }
                helper.assertTrue(propagated && machine.getItem(0).getCount() == 1 && queue(helper, machine) == 0, "Fatal callback error was hidden or consumed input");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerReadinessCannotConsumeTheLiveInput(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "consumingCallback")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.canDisassemble() && machine.getItem(0).getCount() == 1, "Readiness callback consumed live inventory input");
                helper.assertTrue(machine.disassemble() && machine.getItem(0).isEmpty(), "Copied callback input prevented real job start");
                ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(count(chest(helper), Items.EMERALD) == 1, "Consuming callback lost or duplicated its single result");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerSelectorCannotConsumeTheCallersStack(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("consumingSelector", "oneArgument")) {
                final var machine = machine(helper, true); final var input = new ItemStack(Items.DIAMOND);
                helper.assertTrue(machine.canInsert(input) && input.getCount() == 1, "Selector mutated the caller's input stack");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerCanQueueMoreThanNineRecipeParts(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "manyOutputs")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.canDisassemble() && machine.disassemble() && queue(helper, machine) == 12, "Normal startup incorrectly limited a larger job to the internal buffer size");
                ((Connector) machine.node()).changeBuffer(48);
                for (int i = 0; i < 24; i++) tick(helper, machine);
                helper.assertTrue(count(chest(helper), Items.EMERALD) == 12 && queue(helper, machine) == 0 && ((Connector) machine.node()).globalBuffer() == 0D,
                    "Larger queued job lost outputs or changed per-part energy accounting");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerRetainsOldBufferedOutputWhileStartingNewWork(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "oneArgument")) {
                final var machine = machine(helper, true); machine.setItem(1, new ItemStack(Items.STONE)); machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.canDisassemble() && machine.disassemble(), "Existing buffered output blocked a new routable job");
                ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(machine.getItem(1).is(Items.STONE) && machine.getItem(1).getCount() == 1 && count(chest(helper), Items.EMERALD) == 1,
                    "Starting new work overwrote buffered output or lost the new result");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerHonorsInternalSlotLimitsAndDropsOnlyOverflow(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "stackedOutputs")) {
                final var machine = machine(helper, false); final var area = new AABB(machine.getBlockPos()).inflate(3);
                final var existing = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
                machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(machine.disassemble(RandomSource.create(0L), 0D), "Stacked disassembly did not start");
                for (int slot = 1; slot < machine.getContainerSize(); slot++) helper.assertTrue(machine.getItem(slot).getCount() <= machine.getMaxStackSize(), "Internal output exceeded its container slot limit");
                final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, item -> !existing.contains(item.getUUID()) && item.getItem().is(Items.EMERALD));
                helper.assertTrue(count(machine, Items.EMERALD) == 9 && drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == 3, "Stacked internal/overflow outputs lost or duplicated items");
                drops.forEach(ItemEntity::discard);
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerBusyStartDoesNotRepeatImmediateContents(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "bothArrays")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND)); helper.assertTrue(machine.disassemble(), "Fixture did not queue work");
                machine.setItem(0, new ItemStack(Items.DIAMOND));
                helper.assertTrue(!machine.disassemble() && count(chest(helper), Items.PAPER) == 1 && machine.getItem(0).getCount() == 1 && queue(helper, machine) == 1,
                    "Busy job start repeated immediate returns, consumed waiting input or replaced the queue");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerReloadDoesNotRepeatImmediateContents(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "bothArrays")) {
                final var machine = machine(helper, true); machine.setItem(0, new ItemStack(Items.DIAMOND)); helper.assertTrue(machine.disassemble(), "Fixture did not queue work");
                final var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess()); machine.onChunkUnloaded(); machine.setRemoved();
                final var restored = (DisassemblerBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(restored); restored.onLoad(); tick(helper, restored);
                helper.assertTrue(count(chest(helper), Items.PAPER) == 1 && count(chest(helper), Items.EMERALD) == 0 && queue(helper, restored) == 1, "Load repeated immediate contents or released unpaid queued work");
                ((Connector) restored.node()).changeBuffer(4); tick(helper, restored); tick(helper, restored); tick(helper, restored);
                helper.assertTrue(count(chest(helper), Items.PAPER) == 1 && count(chest(helper), Items.EMERALD) == 1, "Restored work lost or repeated returned items");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerExplicitTemplateStillHonorsTheOptOut(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "oneArgument")) {
                final var machine = machine(helper, true); final var input = new ItemStack(Items.DIAMOND);
                final var tag = new CompoundTag(); tag.putBoolean("oc:undisassemblable", true); input.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                helper.assertTrue(!machine.canInsert(input), "Explicit IMC template bypassed the opt-out"); machine.setItem(0, input);
                helper.assertTrue(!machine.disassemble() && machine.getItem(0).getCount() == 1 && count(chest(helper), Items.EMERALD) == 0, "Opt-out input was consumed");
            }
        }); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerConsumesOneUnitOfAnOversizedLoadedStack(GameTestHelper helper) throws Exception {
        context(helper, () -> {
            try (var ignored = register("selectDiamond", "oneArgument")) {
                final var machine = machine(helper, true); final var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
                final var entry = (CompoundTag) new ItemStack(Items.DIAMOND, 2).save(helper.getLevel().registryAccess()); entry.putByte("Slot", (byte) 0);
                final var items = new ListTag(); items.add(entry); saved.put("Items", items); machine.loadWithComponents(saved, helper.getLevel().registryAccess());
                helper.assertTrue(machine.disassemble() && machine.getItem(0).getCount() == 1, "One job consumed every item of an oversized saved input stack");
                ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(machine.getItem(0).getCount() == 1 && count(chest(helper), Items.EMERALD) == 1, "First saved-stack unit was lost or repeated");
                tick(helper, machine); ((Connector) machine.node()).changeBuffer(4); tick(helper, machine); tick(helper, machine);
                helper.assertTrue(machine.getItem(0).isEmpty() && count(chest(helper), Items.EMERALD) == 2, "Second saved-stack unit did not process exactly once");
            }
        }); helper.succeed();
    }

    public static boolean selectDiamond(ItemStack stack) { return stack.is(Items.DIAMOND); }
    public static boolean consumingSelector(ItemStack stack) { final boolean selected = stack.is(Items.DIAMOND); stack.shrink(1); return selected; }
    public static ItemStack[] oneArgument(ItemStack stack) { return new ItemStack[]{new ItemStack(Items.EMERALD)}; }
    public static ItemStack[] consumingCallback(ItemStack stack) { stack.shrink(1); return oneArgument(stack); }
    public static ItemStack[] ordinaryFailure(ItemStack stack) { throw new IllegalArgumentException("expected-imc-callback"); }
    public static ItemStack[] fatalFailure(ItemStack stack) { throw new LinkageError("oc-imc-fatal-probe"); }
    public static Object[] bothArrays(ItemStack stack, ItemStack[] ingredients) { return new Object[]{ingredients, new ItemStack[]{marked(Items.PAPER, 1)}}; }
    public static Object[] singleQueue(ItemStack stack, ItemStack[] ingredients) { return new Object[]{ingredients[0], new ItemStack[]{marked(Items.PAPER, 1)}}; }
    public static Object[] singleDrop(ItemStack stack, ItemStack[] ingredients) { return new Object[]{ingredients, marked(Items.PAPER, 1)}; }
    public static ItemStack[] flatQueue(ItemStack stack, ItemStack[] ingredients) { return ingredients; }
    public static Object[] onlyDrops(ItemStack stack, ItemStack[] ingredients) { return new Object[]{new ItemStack[0], new ItemStack[]{marked(Items.PAPER, 1)}}; }
    public static ItemStack[] bothSignatures(ItemStack stack) { return new ItemStack[]{new ItemStack(Items.IRON_INGOT)}; }
    public static Object[] bothSignatures(ItemStack stack, ItemStack[] ingredients) { return bothArrays(stack, ingredients); }
    public static ItemStack[] manyOutputs(ItemStack stack) { return java.util.stream.IntStream.range(0, 12).mapToObj(i -> new ItemStack(Items.EMERALD)).toArray(ItemStack[]::new); }
    public static ItemStack[] stackedOutputs(ItemStack stack) { return new ItemStack[]{marked(Items.EMERALD, 12)}; }

    private static ItemStack marked(Item item, int count) { final var stack = new ItemStack(item, count); final var tag = new CompoundTag(); tag.putInt("retained", 42); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack; }
    private static CompoundTag payload(String select, String callback) { final var tag = new CompoundTag(); tag.putString("name", "imc_parity_" + callback); tag.putString("select", DisassemblerImcGameTests.class.getName() + "." + select); tag.putString("disassemble", DisassemblerImcGameTests.class.getName() + "." + callback); return tag; }
    private static InterModComms.IMCMessage message(CompoundTag payload) { return new InterModComms.IMCMessage("port_test", NeoOpenComputers.MODID, IMC.REGISTER_DISASSEMBLER_TEMPLATE, () -> payload); }
    private static DisassemblerTemplates.Registration register(String select, String callback) { return DisassemblerTemplateImc.process(java.util.stream.Stream.of(message(payload(select, callback)))).getFirst(); }
    private static DisassemblerBlockEntity machine(GameTestHelper helper, boolean withChest) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(POS)) instanceof DisassemblerBlockEntity old) old.clearContent();
        if (helper.getLevel().getBlockEntity(helper.absolutePos(POS.below())) instanceof ChestBlockEntity old) old.clearContent();
        helper.setBlock(POS, Blocks.AIR); helper.setBlock(POS.below(), Blocks.AIR);
        helper.setBlock(POS, ModBlocks.DISASSEMBLER.get()); if (withChest) helper.setBlock(POS.below(), Blocks.CHEST);
        return helper.getBlockEntity(POS);
    }
    private static ChestBlockEntity chest(GameTestHelper helper) { return helper.getBlockEntity(POS.below()); }
    private static int count(net.minecraft.world.Container inventory, Item item) { int count = 0; for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot).is(item)) count += inventory.getItem(slot).getCount(); return count; }
    private static int queue(GameTestHelper helper, DisassemblerBlockEntity machine) { return machine.saveWithFullMetadata(helper.getLevel().registryAccess()).getList("oc:queue", 10).size(); }
    private static void tick(GameTestHelper helper, DisassemblerBlockEntity machine) { DisassemblerBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine); }
    private static void context(GameTestHelper helper, NeoOpenComputersGameTests.ThrowingRunnable action) throws Exception {
        final var manager = helper.getLevel().getRecipeManager(); final var original = List.copyOf(manager.getRecipes());
        final var recipe = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.DIAMOND), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.EMERALD)));
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "test_imc_ingredients"), recipe)));
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLE_ALL_THE_THINGS, false, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0D, () ->
                    NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 4D, () ->
                        NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_TICK_AMOUNT, 2D, action))));
        } finally { manager.replaceRecipes(original); }
    }
}
