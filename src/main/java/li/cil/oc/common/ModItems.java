package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.item.CpuItem;
import li.cil.oc.common.item.MemoryItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeoOpenComputers.MODID);

    public static final DeferredItem<BlockItem> COMPUTER_CASE_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.COMPUTER_CASE_TIER1,
        ModBlocks.COMPUTER_CASE_TIER1);

    public static final DeferredItem<CpuItem> CPU_TIER1 = ITEMS.register(
        ModContentIds.CPU_TIER1,
        () -> new CpuItem(new Item.Properties()));

    public static final DeferredItem<Item> MANUAL = ITEMS.registerSimpleItem(ModContentIds.MANUAL, new Item.Properties());

    public static final DeferredItem<MemoryItem> MEMORY_TIER1 = ITEMS.register(
        ModContentIds.MEMORY_TIER1,
        () -> new MemoryItem(new Item.Properties()));

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ModItems() {
    }
}
