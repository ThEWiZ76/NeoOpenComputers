package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.common.ManualRegistry;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.item.FloppyItem;
import li.cil.oc.common.item.TabletItem;
import li.cil.oc.common.network.DebugClipboardState;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

@Mod(value = NeoOpenComputers.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = NeoOpenComputers.MODID, value = Dist.CLIENT)
public final class NeoOpenComputersClient {
    private static final List<String> PRELOADED_CLIENT_CLASS_NAMES = List.of(
        "li.cil.oc.client.ManualScreen"
    );
    private static final ResourceLocation NANOMACHINE_HUD = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachine_hud");
    private static final ResourceLocation FLOPPY_COLOR_PROPERTY = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "floppy_color");
    private static final ResourceLocation TABLET_RUNNING_PROPERTY = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "tablet_running");
    private static final int SCREEN_TIER1_COLOR = 0xABABAB;
    private static final int SCREEN_TIER2_COLOR = 0xFFFF66;
    private static final int SCREEN_TIER3_COLOR = 0x66FFFF;

    public NeoOpenComputersClient() {
        NeoForge.EVENT_BUS.addListener(NeoOpenComputersClient::onClientTick);
        NeoForge.EVENT_BUS.register(MfuTargetRenderer.class);
        NeoForge.EVENT_BUS.register(RackMountableRenderHandler.class);
    }

    @SubscribeEvent
    static void onClientSetup(final FMLClientSetupEvent event) {
        preloadClientOnlyClasses();
        if (API.manual instanceof final ManualRegistry manualRegistry) {
            manualRegistry.setLanguageSupplier(() -> Minecraft.getInstance().options.languageCode);
            manualRegistry.setOpenHandler(player -> ManualScreen.open(manualRegistry));
            event.enqueueWork(() -> ManualContent.registerDefaults(manualRegistry));
        }
        event.enqueueWork(() -> ItemProperties.register(
            ModItems.FLOPPY.get(),
            FLOPPY_COLOR_PROPERTY,
            (stack, level, entity, seed) -> FloppyItem.floppyColorIndex(stack)));
        event.enqueueWork(() -> ItemProperties.register(
            ModItems.TABLET.get(),
            TABLET_RUNNING_PROPERTY,
            (stack, level, entity, seed) -> tabletRunningModelProperty(stack)));
        NeoOpenComputers.LOGGER.debug("NeoOpenComputers client setup complete.");
    }

    static void preloadClientOnlyClasses() {
        final ClassLoader loader = NeoOpenComputersClient.class.getClassLoader();
        for (final String className : PRELOADED_CLIENT_CLASS_NAMES) {
            try {
                Class.forName(className, true, loader);
            } catch (final ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @SubscribeEvent
    static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.ADAPTER.get(), AdapterScreen::new);
        event.register(ModMenus.ASSEMBLER.get(), AssemblerScreen::new);
        event.register(ModMenus.CHARGER.get(), ChargerScreen::new);
        event.register(ModMenus.COMPUTER_CASE.get(), ComputerCaseScreen::new);
        event.register(ModMenus.DISASSEMBLER.get(), DisassemblerScreen::new);
        event.register(ModMenus.DISK_DRIVE.get(), DiskDriveScreen::new);
        event.register(ModMenus.DRIVE.get(), DriveScreen::new);
        event.register(ModMenus.PRINTER.get(), PrinterScreen::new);
        event.register(ModMenus.RACK.get(), RackScreen::new);
        event.register(ModMenus.RAID.get(), RaidScreen::new);
        event.register(ModMenus.SERVER_RACK.get(), ServerRackScreen::new);
        event.register(ModMenus.RELAY.get(), RelayScreen::new);
        event.register(ModMenus.TERMINAL.get(), TerminalScreen::new);
        event.register(ModMenus.WAYPOINT.get(), WaypointScreen::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, NANOMACHINE_HUD, (graphics, deltaTracker) -> NanomachineHud.render(graphics, Minecraft.getInstance()));
    }

    @SubscribeEvent
    static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    final Minecraft minecraft = Minecraft.getInstance();
                    renderer = new PrintItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        }, ModItems.PRINT.get());
    }

    @SubscribeEvent
    static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        event.register(
            NeoOpenComputersClient::computerCaseBlockColor,
            ModBlocks.COMPUTER_CASE_TIER1.get(),
            ModBlocks.COMPUTER_CASE_TIER2.get(),
            ModBlocks.COMPUTER_CASE_TIER3.get());
        event.register(
            NeoOpenComputersClient::screenBlockColor,
            ModBlocks.SCREEN_TIER1.get(),
            ModBlocks.SCREEN_TIER2.get(),
            ModBlocks.SCREEN_TIER3.get());
    }

    @SubscribeEvent
    static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
        event.register(
            NeoOpenComputersClient::computerCaseItemColor,
            ModBlocks.COMPUTER_CASE_TIER1.get(),
            ModBlocks.COMPUTER_CASE_TIER2.get(),
            ModBlocks.COMPUTER_CASE_TIER3.get());
        event.register(
            (stack, tintIndex) -> {
                if (tintIndex != 0) {
                    return 0xFFFFFF;
                }
                if (stack.is(ModBlocks.SCREEN_TIER3.get().asItem())) {
                    return SCREEN_TIER3_COLOR;
                }
                if (stack.is(ModBlocks.SCREEN_TIER2.get().asItem())) {
                    return SCREEN_TIER2_COLOR;
                }
                return SCREEN_TIER1_COLOR;
            },
            ModBlocks.SCREEN_TIER1.get(),
            ModBlocks.SCREEN_TIER2.get(),
            ModBlocks.SCREEN_TIER3.get());
    }

    private static int caseTierColor(final Block block) {
        if (block instanceof ComputerCaseBlock computerCase) {
            return tierColor(computerCase.tier());
        }
        return SCREEN_TIER1_COLOR;
    }

    private static int screenTierColor(final Block block) {
        if (block == ModBlocks.SCREEN_TIER3.get()) {
            return tierColor(2);
        }
        if (block == ModBlocks.SCREEN_TIER2.get()) {
            return tierColor(1);
        }
        return tierColor(0);
    }

    static int computerCaseBlockColor(final BlockState state, final BlockAndTintGetter tintGetter, final BlockPos pos, final int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFF;
        }
        return caseTierColor(state.getBlock());
    }

    static int computerCaseItemColor(final ItemStack stack, final int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFF;
        }
        if (stack.is(ModBlocks.COMPUTER_CASE_TIER3.get().asItem())) {
            return tierColor(2);
        }
        if (stack.is(ModBlocks.COMPUTER_CASE_TIER2.get().asItem())) {
            return tierColor(1);
        }
        return tierColor(0);
    }

    static int screenBlockColor(final BlockState state, final BlockAndTintGetter tintGetter, final BlockPos pos, final int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFF;
        }
        if (tintGetter != null && pos != null && tintGetter.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
            return screen.getRenderColor();
        }
        return screenTierColor(state.getBlock());
    }

    private static int tierColor(final int tier) {
        return switch (tier) {
            case 2 -> SCREEN_TIER3_COLOR;
            case 1 -> SCREEN_TIER2_COLOR;
            default -> SCREEN_TIER1_COLOR;
        };
    }

    static float tabletRunningModelProperty(final ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof TabletItem tablet) || !tablet.hasData(stack)) {
            return -1F;
        }
        return tablet.isRunning(stack) ? 1F : 0F;
    }

    static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final String clipboard = DebugClipboardState.consumePendingClipboard();
        if (clipboard != null) {
            minecraft.keyboardHandler.setClipboard(clipboard);
        }
        NanomachineParticles.spawnAmbient(minecraft);
        ComputerCaseSounds.tick();
    }

    @SubscribeEvent
    static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ADAPTER.get(), AdapterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ASSEMBLER.get(), AssemblerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGER.get(), ChargerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.COMPUTER_CASE.get(), ComputerCaseBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISASSEMBLER.get(), DisassemblerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISK_DRIVE.get(), DiskDriveBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GEOLYZER.get(), GeolyzerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HOLOGRAM.get(), HologramBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NET_SPLITTER.get(), NetSplitterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POWER_DISTRIBUTOR.get(), PowerDistributorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PRINTER.get(), PrinterBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PRINT.get(), PrintBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RACK.get(), RackBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RAID.get(), RaidBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RELAY.get(), RelayBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ScreenBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TRANSPOSER.get(), TransposerBlockEntityRenderer::new);
    }
}
