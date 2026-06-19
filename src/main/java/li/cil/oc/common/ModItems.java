package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.item.CpuItem;
import li.cil.oc.common.item.EepromItem;
import li.cil.oc.common.item.GraphicsCardItem;
import li.cil.oc.common.item.HardDiskDriveItem;
import li.cil.oc.common.item.MemoryItem;
import li.cil.oc.common.item.NetworkCardItem;
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

    public static final DeferredItem<EepromItem> EEPROM = ITEMS.register(
        ModContentIds.EEPROM,
        () -> new EepromItem(new Item.Properties()));

    public static final DeferredItem<GraphicsCardItem> GRAPHICS_CARD_TIER1 = ITEMS.register(
        ModContentIds.GRAPHICS_CARD_TIER1,
        () -> new GraphicsCardItem(new Item.Properties()));

    public static final DeferredItem<HardDiskDriveItem> HDD_TIER1 = ITEMS.register(
        ModContentIds.HDD_TIER1,
        () -> new HardDiskDriveItem(new Item.Properties()));

    public static final DeferredItem<Item> MANUAL = ITEMS.registerSimpleItem(ModContentIds.MANUAL, new Item.Properties());

    public static final DeferredItem<MemoryItem> MEMORY_TIER1 = ITEMS.register(
        ModContentIds.MEMORY_TIER1,
        () -> new MemoryItem(new Item.Properties()));

    public static final DeferredItem<NetworkCardItem> NETWORK_CARD = ITEMS.register(
        ModContentIds.NETWORK_CARD,
        () -> new NetworkCardItem(new Item.Properties()));

    public static final DeferredItem<BlockItem> SCREEN_TIER1 = ITEMS.registerSimpleBlockItem(
        ModContentIds.SCREEN_TIER1,
        ModBlocks.SCREEN_TIER1);

    public static final DeferredItem<BlockItem> KEYBOARD = ITEMS.registerSimpleBlockItem(
        ModContentIds.KEYBOARD,
        ModBlocks.KEYBOARD);

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ModItems() {
    }
}
