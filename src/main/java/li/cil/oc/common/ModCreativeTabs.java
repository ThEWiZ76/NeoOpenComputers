package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NeoOpenComputers.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(ModContentIds.MAIN_CREATIVE_TAB, () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.neoopencomputers"))
            .icon(() -> new ItemStack(ModItems.MANUAL.get()))
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    public static void register(final IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    public static void assignApiCreativeTab() {
        li.cil.oc.api.CreativeTab.instance = MAIN.get();
    }

    private ModCreativeTabs() {
    }
}
