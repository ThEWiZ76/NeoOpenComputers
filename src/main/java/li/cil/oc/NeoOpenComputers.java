package li.cil.oc;

import com.mojang.logging.LogUtils;
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
        modEventBus.addListener(this::onCommonSetup);
        LOGGER.info("Loading NeoOpenComputers {}", modContainer.getModInfo().getVersion());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("NeoOpenComputers common setup complete.");
    }
}
