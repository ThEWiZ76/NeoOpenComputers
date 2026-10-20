package li.cil.oc.client;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class OreDictImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(final String data) {
        final ResourceLocation id = parseTagId(data);
        final List<ItemStack> stacks = id == null ? List.of() : BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
            .stream()
            .flatMap(holders -> holders.stream())
            .map(holder -> new ItemStack(holder.value()))
            .filter(stack -> !stack.isEmpty())
            .toList();
        if (!stacks.isEmpty()) {
            return new ItemStackImageRenderer(stacks);
        }
        if (data != null && data.startsWith("oc:")) {
            final var info = li.cil.oc.api.Items.get(data.substring(3));
            final ItemStack stack = info == null ? null : info.createItemStack(1);
            if (stack != null && !stack.isEmpty()) {
                return new ItemStackImageRenderer(List.of(stack));
            }
        }
        return missing();
    }

    private static ResourceLocation parseTagId(final String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return data.indexOf(':') >= 0 ? ResourceLocation.parse(data) : ResourceLocation.fromNamespaceAndPath("forge", data);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static ImageRenderer missing() {
        return new MissingManualImageRenderer("oc:gui.Manual.Warning.OreDictMissing");
    }
}
