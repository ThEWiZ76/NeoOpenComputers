package li.cil.oc;

import com.mojang.logging.LogUtils;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModContentCatalog;
import li.cil.oc.common.ModCreativeTabs;
import li.cil.oc.common.ModDriverCatalog;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.OpenComputersApi;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(NeoOpenComputers.MODID)
public final class NeoOpenComputers {
    public static final String MODID = "neoopencomputers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeoOpenComputers(final IEventBus modEventBus, final ModContainer modContainer) {
        OpenComputersApi.initialize();
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);
        LOGGER.info("Loading NeoOpenComputers {}", modContainer.getModInfo().getVersion());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModCreativeTabs.assignApiCreativeTab();
            ModContentCatalog.registerDefaults();
            ModDriverCatalog.registerDefaults();
        });
        LOGGER.debug("NeoOpenComputers common setup complete.");
    }
}
