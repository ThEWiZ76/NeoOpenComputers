package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tiered;
import net.minecraft.world.item.Item;

public class MicrocontrollerCaseItem extends Item implements Tiered {
    private final int tier;

    public MicrocontrollerCaseItem(final Properties properties, final int tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }
}
