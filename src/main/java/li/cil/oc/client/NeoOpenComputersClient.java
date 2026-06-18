package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = NeoOpenComputers.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoOpenComputers.MODID, value = Dist.CLIENT)
public final class NeoOpenComputersClient {
    public NeoOpenComputersClient() {
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        NeoOpenComputers.LOGGER.debug("NeoOpenComputers client setup complete.");
    }
}
