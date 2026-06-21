package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

    @SubscribeEvent
    static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPUTER_CASE.get(), ComputerCaseScreen::new);
        event.register(ModMenus.DISK_DRIVE.get(), DiskDriveScreen::new);
        event.register(ModMenus.RACK.get(), RackScreen::new);
        event.register(ModMenus.RAID.get(), RaidScreen::new);
        event.register(ModMenus.RELAY.get(), RelayScreen::new);
    }

    @SubscribeEvent
    static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ScreenBlockEntityRenderer::new);
    }
}
