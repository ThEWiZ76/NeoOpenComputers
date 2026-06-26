package li.cil.oc;

import com.mojang.logging.LogUtils;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModCapabilities;
import li.cil.oc.common.ModContentCatalog;
import li.cil.oc.common.ModCreativeTabs;
import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.HostBlacklistImc;
import li.cil.oc.common.InkProviderImc;
import li.cil.oc.common.ModDriverCatalog;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ItemChargeImc;
import li.cil.oc.common.ModInkProviders;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModItemCharges;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.ModRecipeSerializers;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.ModWrenches;
import li.cil.oc.common.NanomachinesRegistry;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.PeripheralBlacklistImc;
import li.cil.oc.common.ToolDurabilityProviderImc;
import li.cil.oc.common.WrenchToolImc;
import li.cil.oc.common.component.ChunkloaderUpgradeEnvironment;
import li.cil.oc.common.component.FileSystemAccessHandler;
import li.cil.oc.common.component.MfuTargetEvents;
import li.cil.oc.common.command.ModCommands;
import li.cil.oc.common.machine.ProgramLocationImc;
import li.cil.oc.common.machine.ProgramLocations;
import li.cil.oc.common.network.DebugNetworking;
import li.cil.oc.common.network.NanomachinesNetworking;
import li.cil.oc.common.network.RackNetworking;
import li.cil.oc.common.network.TerminalNetworking;
import li.cil.oc.common.template.AssemblerFilterImc;
import li.cil.oc.common.template.AssemblerTemplateImc;
import li.cil.oc.common.template.DisassemblerTemplateImc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
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
        ModRecipeSerializers.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModSettings.SPEC);
        modEventBus.addListener(ModCapabilities::register);
        FileSystemAccessHandler.register();
        MfuTargetEvents.register();
        NeoForge.EVENT_BUS.addListener(ModCommands::register);
        NanomachinesRegistry.registerTickHandler();
        modEventBus.addListener(ChunkloaderUpgradeEnvironment::registerTicketController);
        modEventBus.addListener(DebugNetworking::register);
        modEventBus.addListener(NanomachinesNetworking::register);
        modEventBus.addListener(RackNetworking::register);
        modEventBus.addListener(TerminalNetworking::register);
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onInterModProcess);
        LOGGER.info("Loading NeoOpenComputers {}", modContainer.getModInfo().getVersion());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModCreativeTabs.assignApiCreativeTab();
            ModContentCatalog.registerDefaults();
            ModEeproms.registerDefaults();
            ModInkProviders.registerDefaults();
            ModItemCharges.registerDefaults();
            ModWrenches.registerDefaults();
            ModLootDisks.registerDefaults();
            ProgramLocations.registerDefaults();
            ModDriverCatalog.registerDefaults();
            OpenComputersApi.lockDriverRegistry();
        });
        LOGGER.debug("NeoOpenComputers common setup complete.");
    }

    private void onInterModProcess(final InterModProcessEvent event) {
        event.enqueueWork(() -> {
            final var messages = InterModComms.getMessages(MODID).toList();
            ProgramLocationImc.process(messages.stream());
            InkProviderImc.process(messages.stream());
            ItemChargeImc.process(messages.stream());
            PeripheralBlacklistImc.process(messages.stream());
            ToolDurabilityProviderImc.process(messages.stream());
            WrenchToolImc.process(messages.stream());
            AssemblerFilterImc.process(messages.stream());
            AssemblerTemplateImc.process(messages.stream());
            DisassemblerTemplateImc.process(messages.stream());
            if (li.cil.oc.api.API.driver instanceof DriverRegistry registry) {
                HostBlacklistImc.process(registry, messages.stream());
            }
        });
    }
}
