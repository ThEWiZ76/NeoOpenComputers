package li.cil.oc.api.detail;

import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ManualAPI {
    void addTab(TabIconRenderer renderer, String tooltip, String path);

    void addProvider(PathProvider provider);

    void addProvider(ContentProvider provider);

    void addProvider(String prefix, ImageProvider provider);

    String pathFor(ItemStack stack);

    String pathFor(Level world, BlockPos pos);

    Iterable<String> contentFor(String path);

    ImageRenderer imageFor(String path);

    void openFor(Player player);

    void reset();

    void navigate(String path);
}
