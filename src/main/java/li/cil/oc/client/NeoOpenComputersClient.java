package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.common.ManualRegistry;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.network.DebugClipboardState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = NeoOpenComputers.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoOpenComputers.MODID, value = Dist.CLIENT)
public final class NeoOpenComputersClient {
    private static final ResourceLocation NANOMACHINE_HUD = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachine_hud");

    public NeoOpenComputersClient() {
        NeoForge.EVENT_BUS.addListener(NeoOpenComputersClient::onClientTick);
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        if (API.manual instanceof final ManualRegistry manualRegistry) {
            manualRegistry.setLanguageSupplier(() -> Minecraft.getInstance().options.languageCode);
            event.enqueueWork(() -> ManualContent.registerDefaults(manualRegistry));
        }
        NeoOpenComputers.LOGGER.debug("NeoOpenComputers client setup complete.");
    }

    @SubscribeEvent
    static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.ASSEMBLER.get(), AssemblerScreen::new);
        event.register(ModMenus.COMPUTER_CASE.get(), ComputerCaseScreen::new);
        event.register(ModMenus.DISASSEMBLER.get(), DisassemblerScreen::new);
        event.register(ModMenus.DISK_DRIVE.get(), DiskDriveScreen::new);
        event.register(ModMenus.RACK.get(), RackScreen::new);
        event.register(ModMenus.RAID.get(), RaidScreen::new);
        event.register(ModMenus.SERVER_RACK.get(), ServerRackScreen::new);
        event.register(ModMenus.RELAY.get(), RelayScreen::new);
        event.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, NANOMACHINE_HUD, (graphics, deltaTracker) -> NanomachineHud.render(graphics, Minecraft.getInstance()));
    }

    static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final String clipboard = DebugClipboardState.consumePendingClipboard();
        if (clipboard != null) {
            minecraft.keyboardHandler.setClipboard(clipboard);
        }
        NanomachineParticles.spawnAmbient(minecraft);
    }

    @SubscribeEvent
    static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ScreenBlockEntityRenderer::new);
    }
}
