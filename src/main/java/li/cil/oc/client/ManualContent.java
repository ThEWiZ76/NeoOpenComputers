package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import li.cil.oc.api.prefab.TextureTabIconRenderer;
import li.cil.oc.common.ManualRegistry;
import li.cil.oc.common.ModContentIds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ManualContent {
    private static final ResourceLocation HOME_TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/gui/manual_home.png");

    public static void registerDefaults(final ManualRegistry registry) {
        registry.addProvider(new DefinitionPathProvider());
        registry.addProvider(new ResourceContentProvider(NeoOpenComputers.MODID, "doc/"));
        registry.addProvider("", new TextureImageProvider());
        registry.addProvider("item", new ItemImageProvider());
        registry.addProvider("block", new BlockImageProvider());
        registry.addProvider("oredict", new OreDictImageProvider());

        registry.addTab(new TextureTabIconRenderer(HOME_TAB_TEXTURE), "oc:gui.Manual.Home", "%LANGUAGE%/index.md");
        registry.addTab(new ItemStackTabIconRenderer(stackFor(ModContentIds.COMPUTER_CASE_TIER1)), "oc:gui.Manual.Blocks", "%LANGUAGE%/block/index.md");
        registry.addTab(new ItemStackTabIconRenderer(stackFor(ModContentIds.CPU_TIER1)), "oc:gui.Manual.Items", "%LANGUAGE%/item/index.md");
    }

    private static ItemStack stackFor(final String name) {
        final ItemInfo info = API.items == null ? null : API.items.get(name);
        return info == null ? null : info.createItemStack(1);
    }

    private static final class DefinitionPathProvider implements li.cil.oc.api.manual.PathProvider {
        @Override
        public String pathFor(final ItemStack stack) {
            final ItemInfo info = API.items == null ? null : API.items.get(stack);
            return pathFor(info);
        }

        @Override
        public String pathFor(final Level world, final BlockPos pos) {
            if (world == null || pos == null || API.items == null) {
                return null;
            }
            return pathFor(API.items.get(new ItemStack(world.getBlockState(pos).getBlock().asItem())));
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
