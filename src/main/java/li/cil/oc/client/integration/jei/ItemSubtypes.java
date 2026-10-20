package li.cil.oc.client.integration.jei;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ItemRegistry;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

final class ItemSubtypes {
    static final ISubtypeInterpreter<ItemStack> COMPONENTS = new ISubtypeInterpreter<>() {
        @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
            final var patch = stack.getComponentsPatch();
            return patch.isEmpty() ? null : patch;
        }

        @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) { return ""; }
    };

    static final ISubtypeInterpreter<ItemStack> FLOPPY = new ISubtypeInterpreter<>() {
        @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
            final var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) return null;
            final var tag = data.copyTag();
            // Keep the same precedence as FloppyItem when old and new fields coexist.
            if (tag.contains("oc:lootPath")) return NeoOpenComputers.MODID + ":loot/" + tag.getString("oc:lootPath");
            if (tag.contains("oc:lootFactory")) return tag.getString("oc:lootFactory");
            return tag.contains(ItemRegistry.FLOPPY_FACTORY_ID_TAG) ? tag.getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG) : null;
        }

        @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) { return ""; }
    };

    private ItemSubtypes() {}
}
