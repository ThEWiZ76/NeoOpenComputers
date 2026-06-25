package li.cil.oc.common.item;

import li.cil.oc.common.item.data.PrintData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PrintItem extends BlockItem {
    public PrintItem(final Block block, final Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        final ItemStack stack,
        final Item.TooltipContext context,
        final List<Component> tooltip,
        final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        final PrintData data = new PrintData(stack);
        if (data.tooltip() != null) {
            data.tooltip().lines()
                .map(Component::literal)
                .forEach(tooltip::add);
        }
        if (data.isBeaconBase()) {
            tooltip.add(Component.translatable("tooltip.neoopencomputers.print.beacon_base"));
        }
        if (data.emitRedstone()) {
            tooltip.add(Component.translatable("tooltip.neoopencomputers.print.redstone_level", data.redstoneLevel()));
        }
        if (data.emitLight()) {
            tooltip.add(Component.translatable("tooltip.neoopencomputers.print.light_level", data.lightLevel()));
        }
    }
}
