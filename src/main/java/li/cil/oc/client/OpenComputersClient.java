package li.cil.oc.client;

import li.cil.oc.OpenComputers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = OpenComputers.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = OpenComputers.MODID, value = Dist.CLIENT)
public final class OpenComputersClient {
    public OpenComputersClient() {
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        OpenComputers.LOGGER.debug("OpenComputers client setup complete.");
    }
}
