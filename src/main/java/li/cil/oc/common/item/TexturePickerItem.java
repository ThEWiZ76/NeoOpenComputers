package li.cil.oc.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TexturePickerItem extends Item {
    public TexturePickerItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final String texture = describeBlockTexture(context.getLevel(), context.getClickedPos());
        if (texture.isEmpty()) {
            return InteractionResult.PASS;
        }
        final Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide()) {
            player.sendSystemMessage(Component.literal("Texture: " + texture));
        }
        return InteractionResult.CONSUME;
    }

    public static String describeBlockTexture(final Level level, final BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return "";
        }
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return "";
        }
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == BuiltInRegistries.BLOCK.getKey(Blocks.AIR) ? "" : id.toString();
    }
}
