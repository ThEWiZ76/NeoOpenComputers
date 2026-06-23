package li.cil.oc.client;

import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class BlockImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(final String data) {
        final ParsedImageTarget target = ParsedImageTarget.parse(data);
        if (target == null) {
            return missing();
        }

        final Block block = BuiltInRegistries.BLOCK.get(target.id());
        if (block == Blocks.AIR) {
            return missing();
        }
        final Item item = block.asItem();
        if (item == Items.AIR) {
            return missing();
        }
        return new ItemStackImageRenderer(List.of(new ItemStack(item)));
    }

    private static ImageRenderer missing() {
        return new MissingManualImageRenderer("oc:gui.Manual.Warning.BlockMissing");
    }
}
