package li.cil.oc.common.item;

import net.minecraft.world.item.Item;

public class TerminalItem extends Item {
    public TerminalItem(final Properties properties) {
        super(properties.stacksTo(1));
    }
}
