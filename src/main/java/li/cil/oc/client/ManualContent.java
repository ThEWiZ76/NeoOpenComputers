package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.api.prefab.TextureTabIconRenderer;
import li.cil.oc.common.ManualRegistry;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModContentCatalog;
import li.cil.oc.common.ModContentIds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Set;

public final class ManualContent {
    private static final ResourceLocation HOME_TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual_home.png");
    private static final ResourceLocation BLOCKS_TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual_blocks.png");
    private static final ResourceLocation ITEMS_TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual_items.png");

    public static void registerDefaults(final ManualRegistry registry) {
        registry.addProvider(new DefinitionPathProvider());
        registry.addProvider(new ResourceContentProvider(NeoOpenComputers.MODID, "doc/"));
        registry.addProvider("", new TextureImageProvider());
        registry.addProvider("item", new ItemImageProvider());
        registry.addProvider("block", new BlockImageProvider());
        registry.addProvider("oredict", new OreDictImageProvider());

        registry.addTab(new TextureTabIconRenderer(HOME_TAB_TEXTURE), "oc:gui.Manual.Home", "%LANGUAGE%/index.md");
        registry.addTab(new TextureTabIconRenderer(BLOCKS_TAB_TEXTURE), "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md");
        registry.addTab(new TextureTabIconRenderer(ITEMS_TAB_TEXTURE), "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md");
    }

    private static final class DefinitionPathProvider implements li.cil.oc.api.manual.PathProvider {
        private static final Set<String> EXCLUDED = Set.of(
            ModContentIds.DEBUGGER, ModContentIds.DIAMOND_CHIP,
            ModContentCatalog.COMPAT_DIAMOND_CHIP_UPSTREAM, ModContentIds.ENDSTONE);

        @Override
        public String pathFor(final ItemStack stack) {
            final ItemInfo info = API.items == null ? null : API.items.get(stack);
            if (info == null || EXCLUDED.contains(info.name())) {
                return null;
            }
            if (API.items instanceof ItemRegistry registry) {
                for (final var alias : registry.aliasesFor(stack)) {
                    final String path = pathFor(alias);
                    final String resource = "/assets/" + NeoOpenComputers.MODID + "/doc/"
                        + path.replace("%LANGUAGE%", "en_us").toLowerCase(Locale.ROOT);
                    if (ManualContent.class.getResource(resource) != null) {
                        return path;
                    }
                }
            }
            return pathFor(info);
        }

        @Override
        public String pathFor(final Level world, final BlockPos pos) {
            if (world == null || pos == null || API.items == null) {
                return null;
            }
            return pathFor(new ItemStack(world.getBlockState(pos).getBlock().asItem()));
        }

        private static String pathFor(final ItemInfo info) {
            if (info == null) {
                return null;
            }
            return info.block() == null ? "%LANGUAGE%/item/" + info.name() + ".md" : "%LANGUAGE%/block/" + info.name() + ".md";
        }
    }

    private ManualContent() {
    }
}
