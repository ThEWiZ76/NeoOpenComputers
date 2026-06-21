package li.cil.oc;

import com.mojang.logging.LogUtils;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModContentCatalog;
import li.cil.oc.common.ModCreativeTabs;
import li.cil.oc.common.ModDriverCatalog;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.machine.ProgramLocationImc;
import li.cil.oc.common.network.TerminalNetworking;
import li.cil.oc.common.template.AssemblerTemplateImc;
import li.cil.oc.common.template.DisassemblerTemplateImc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
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
            ModLootDisks.registerDefaults();
            ModDriverCatalog.registerDefaults();
        });
        LOGGER.debug("NeoOpenComputers common setup complete.");
    }

    private void onInterModProcess(final InterModProcessEvent event) {
        event.enqueueWork(() -> {
            final var messages = InterModComms.getMessages(MODID).toList();
            ProgramLocationImc.process(messages.stream());
            AssemblerTemplateImc.process(messages.stream());
            DisassemblerTemplateImc.process(messages.stream());
        });
    }
}
