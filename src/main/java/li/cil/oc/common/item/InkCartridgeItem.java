package li.cil.oc.common.item;

import net.minecraft.world.item.Item;

public class InkCartridgeItem extends Item {
    public InkCartridgeItem(final Properties properties, final Item emptyCartridge) {
        super(properties.stacksTo(1).craftRemainder(emptyCartridge));
    }
}
