package li.cil.oc;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(OpenComputers.MODID)
public final class OpenComputers {
    public static final String MODID = "opencomputers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenComputers(final IEventBus modEventBus, final ModContainer modContainer) {
        modEventBus.addListener(this::onCommonSetup);
        LOGGER.info("Loading OpenComputers NeoForge port {}", modContainer.getModInfo().getVersion());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("OpenComputers common setup complete.");
    }
}
