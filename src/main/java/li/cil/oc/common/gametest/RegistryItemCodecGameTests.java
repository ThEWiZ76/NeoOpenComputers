package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.item.TabletRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RegistryItemCodecGameTests {
    @GameTest(template = "empty")
    public static void tabletAssemblerRuntimeAndSaveRetainRegistryBoundComponents(GameTestHelper helper) {
        final var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        final var unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
        final var memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        final var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY); enchantments.set(unbreaking, 1);
        memory.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        final var marker = new CompoundTag(); marker.putInt("retained", 42); memory.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        final var pos = new BlockPos(1, 1, 1); helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(ModItems.TABLET_CASE_TIER2.get()));
        final var container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
        container.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        assembler.setItem(AssemblerBlockEntity.SLOT_CONTAINER_START, container);
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, new ItemStack(ModItems.CPU_TIER1.get()));
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, memory);
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 2, RobotMovementPersistenceGameTests.eeprom("return"));
        helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Physical tablet assembler rejected the fixture");
        final var tablet = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);
        final var stored = storedMemory(helper, tablet);
        helper.assertTrue(stored.is(ModItems.MEMORY_TIER1.get()) && stored.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(unbreaking) == 1,
            "Physical tablet assembler discarded registry-bound RAM before runtime creation");
        final var runtime = new TabletRuntime(tablet, helper.makeMockPlayer(GameType.SURVIVAL));
        try {
            helper.assertTrue(runtime.expansionInventory().canPlaceItem(0, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get())),
                "Registry-bound container data prevented valid editor expansion");
            final var expansion = new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get());
            expansion.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
            runtime.expansionInventory().setItem(0, expansion);
            helper.assertTrue(ModItems.TABLET.get().getComponent(tablet, 31, helper.getLevel().registryAccess())
                .getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(unbreaking) == 1,
                "Tablet editor save lost registry-bound expansion data");
            ItemStack loaded = ItemStack.EMPTY;
            for (var component : runtime.internalComponents()) if (component.is(ModItems.MEMORY_TIER1.get())) loaded = component;
            helper.assertTrue(loaded.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(unbreaking) == 1
                && loaded.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("retained") == 42,
                "Tablet runtime dropped registry-bound component data while loading");
        } finally { runtime.close(false); }
        final var saved = storedMemory(helper, tablet);
        helper.assertTrue(saved.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(unbreaking) == 1
            && saved.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("retained") == 42,
            "Tablet runtime save erased registry-bound component data");
        final var disassemblerPos = new BlockPos(0, 1, 0); helper.setBlock(disassemblerPos, ModBlocks.DISASSEMBLER.get());
        final DisassemblerBlockEntity disassembler = helper.getBlockEntity(disassemblerPos); disassembler.setItem(0, tablet.copy());
        helper.assertTrue(disassembler.disassemble(RandomSource.create(0L), 0D), "Registry-bound tablet disassembly did not start");
        int restoredMemory = 0, restoredContainer = 0, restoredExpansion = 0;
        for (int slot = 1; slot < disassembler.getContainerSize(); slot++) {
            final var part = disassembler.getItem(slot);
            if (part.is(ModItems.MEMORY_TIER1.get()) || part.is(ModItems.CARD_CONTAINER_TIER1.get()) || part.is(ModItems.WIRELESS_NETWORK_CARD_TIER1.get())) {
                helper.assertTrue(part.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(unbreaking) == 1,
                    "Tablet disassembly lost registry-bound part data");
                if (part.is(ModItems.MEMORY_TIER1.get())) restoredMemory += part.getCount();
                else if (part.is(ModItems.CARD_CONTAINER_TIER1.get())) restoredContainer += part.getCount();
                else restoredExpansion += part.getCount();
            }
        }
        helper.assertTrue(restoredMemory == 1 && restoredContainer == 1 && restoredExpansion == 1, "Tablet disassembly lost or duplicated registry-bound parts");
        helper.succeed();
    }

    private static ItemStack storedMemory(GameTestHelper helper, ItemStack tablet) {
        final var data = tablet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("oc:tablet");
        final var parts = data.getList("components", Tag.TAG_COMPOUND);
        for (int i = 0; i < parts.size(); i++) {
            final var entry = parts.getCompound(i);
            if (entry.getInt("slot") == 2) return ItemStack.parseOptional(helper.getLevel().registryAccess(), entry.getCompound("stack"));
        }
        return ItemStack.EMPTY;
    }
}
